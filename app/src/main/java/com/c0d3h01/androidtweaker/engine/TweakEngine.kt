package com.c0d3h01.androidtweaker.engine

import android.content.Context
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.CommandRunner
import com.c0d3h01.androidtweaker.profiles.Profile
import com.c0d3h01.androidtweaker.profiles.ProfilePreferences
import com.c0d3h01.androidtweaker.profiles.ProfileSettingsStore
import com.c0d3h01.androidtweaker.tuning.TuningStore
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Runs the bundled tweak engine and records its results in app-private storage.
 */
object TweakEngine {
  const val ENGINE_VERSION = "5"
  private val operations = Mutex()
  private var stagedAssets: String? = null

  data class Result(
    val ok: Boolean,
    val exitCode: Int,
    val output: String,
    val logPath: String,
    val at: Long = System.currentTimeMillis(),
  )

  suspend fun apply(
    context: Context,
    runner: CommandRunner,
    profile: Profile,
    fromBoot: Boolean = false,
  ): Result = execute(context, runner, "apply ${profile.name.lowercase(Locale.US)}", profile, fromBoot)

  suspend fun misc(
    context: Context,
    runner: CommandRunner,
  ): Result = execute(context, runner, "misc")

  /**
   * Runs one dispatcher command ("charge", "clean cache", "doze force", ...)
   * after mirroring the current feature switches to disk.
   */
  suspend fun command(
    context: Context,
    runner: CommandRunner,
    args: String,
  ): Result = execute(context, runner, args)

  private suspend fun execute(
    context: Context,
    runner: CommandRunner,
    args: String,
    profile: Profile? = null,
    fromBoot: Boolean = false,
  ): Result =
    withContext(Dispatchers.IO) {
      operations.withLock {
        if (fromBoot && (profile == null || !ProfilePreferences.canReapply(context, profile.code))) {
          return@withLock Result(false, 125, "Boot reapply disabled or profile changed", EngineFiles.logFile(context).absolutePath)
        }
        val mode = AccessManager.modeOf(runner)
        val rootRequired = EngineCommandPolicy.commandRequiresRoot(args)
        if (rootRequired && mode == AccessManager.Mode.SHIZUKU) {
          return@withLock Result(false, 126, "Root access required", EngineFiles.logFile(context).absolutePath)
        }
        val dir = EngineFiles.ensure(context)
        val time = EngineFiles.stamp()
        val staged = mode == AccessManager.Mode.SHIZUKU
        val command =
          if (staged) {
            stagedCommand(context, args, profile, time)
          } else {
            if (profile != null) checkNotNull(ProfileSettingsStore.writeConf(context, profile)) { "Could not write profile configuration" }
            checkNotNull(TuningStore.writeConf(context)) { "Could not write feature configuration" }
            EngineCommands.run(dir.absolutePath, args, time)
          }
        val raw = runner.run(if (rootRequired) requireRoot(command) else command)
        finish(context, raw).also { result ->
          if (profile != null && result.ok && !fromBoot) ProfilePreferences.confirm(context, profile.code, mode)
          if (staged) EngineFiles.record(context, "${mode.label} $args (exit ${result.exitCode})\n${result.output}")
        }
      }
    }

  private fun requireRoot(command: String): String =
    "if [ \"\$(id -u)\" != 0 ]; then printf 'Root access required\\n${EngineCommands.EXIT_MARKER}126\\n'; exit 126; fi\n$command"

  private fun stagedCommand(
    context: Context,
    args: String,
    profile: Profile?,
    time: String,
  ): String =
    buildString {
      // A Shizuku shell may be denied access to the app's internal files by SELinux.
      appendLine("umask 077")
      appendLine("MODDIR=\$(mktemp -d /data/local/tmp/.androidtweaker.XXXXXX) || exit 1")
      appendLine("trap 'rm -rf -- \"\$MODDIR\"' EXIT")
      appendLine("trap 'exit 143' HUP INT TERM")
      appendLine("mkdir -p \"\$MODDIR/custom\" || exit 1")
      val assets = stagedAssets ?: buildString { stageAssets(context, this, "") }.also { stagedAssets = it }
      append(assets)
      appendLine(writeStagedFile("custom/features.conf", TuningStore.load(context).conf()))
      if (profile != null) {
        val path = "custom/${profile.name.lowercase(Locale.US)}.conf"
        appendLine(writeStagedFile(path, ProfileSettingsStore.load(context, profile).conf()))
      }
      appendLine("cd \"\$MODDIR\" || exit 1")
      append(EngineCommands.run(".", args, time))
    }

  private fun stageAssets(context: Context, script: StringBuilder, dir: String) {
    for (name in context.assets.list("${EngineFiles.ASSET_ROOT}$dir").orEmpty()) {
      val child = "$dir/$name"
      val path = "${EngineFiles.ASSET_ROOT}$child"
      if (context.assets.list(path).orEmpty().isNotEmpty()) {
        script.appendLine("mkdir -p \"\$MODDIR\"${EngineCommands.quote(child)} || exit 1")
        stageAssets(context, script, child)
      } else {
        val content = context.assets.open(path).bufferedReader().use { it.readText() }
        script.appendLine(writeStagedFile(child.removePrefix("/"), content))
      }
    }
  }

  private fun writeStagedFile(name: String, content: String): String =
    "printf '%s' ${EngineCommands.quote(content)} > \"\$MODDIR/\"${EngineCommands.quote(name)} || exit 1"

  private fun finish(
    context: Context,
    raw: String,
  ): Result {
    val parsed = EngineCommands.parse(raw)
    val log = EngineFiles.logFile(context)
    return Result(
      ok = parsed.exitCode == 0,
      exitCode = parsed.exitCode,
      output = parsed.output,
      logPath = log.absolutePath,
    )
  }

}
