package com.c0d3h01.androidtweaker.data

import android.content.Context
import com.c0d3h01.androidtweaker.util.ShellAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Runs the bundled tweak engine and records its results in app-private storage.
 */
object TweakEngine {
  const val ENGINE_VERSION = "5"
  const val LOG_NAME = "tweaks.log"
  private const val ASSET_ROOT = "tweaker"
  private val operations = Mutex()
  private var stagedAssets: String? = null

  data class Result(
    val ok: Boolean,
    val exitCode: Int,
    val output: String,
    val logPath: String,
    val at: Long = System.currentTimeMillis(),
  )

  fun directory(context: Context): File = File(context.filesDir, ASSET_ROOT)

  fun logFile(context: Context): File = File(directory(context), LOG_NAME)

  fun ready(context: Context): Boolean = File(directory(context), "lib/dispatcher.sh").isFile

  fun record(
    context: Context,
    line: String,
  ) {
    runCatching {
      val log = logFile(context)
      log.parentFile?.mkdirs()
      log.appendText("${stamp()} $line\n")
    }
  }

  private fun ensure(context: Context): File {
    val dir = directory(context)
    val marker = File(dir, ".engine-$ENGINE_VERSION")
    if (!marker.isFile || !ready(context)) {
      dir.mkdirs()
      copyAssets(context, ASSET_ROOT, dir)
      marker.writeText(ENGINE_VERSION)
    }
    ensureLog(context)
    return dir
  }

  private fun ensureLog(context: Context) {
    val log = logFile(context)
    if (!log.exists()) {
      runCatching {
        log.parentFile?.mkdirs()
        log.createNewFile()
        log.setReadable(true, false)
        log.setWritable(true, false)
      }
    }
  }

  suspend fun apply(
    context: Context,
    runner: ShellRunner,
    profile: Profile,
    fromBoot: Boolean = false,
  ): Result = execute(context, runner, "apply ${profile.name.lowercase(Locale.US)}", profile, fromBoot)

  suspend fun misc(
    context: Context,
    runner: ShellRunner,
  ): Result = execute(context, runner, "misc")

  /**
   * Runs one dispatcher command ("charge", "clean cache", "doze force", ...)
   * after mirroring the current feature switches to disk.
   */
  suspend fun command(
    context: Context,
    runner: ShellRunner,
    args: String,
  ): Result = execute(context, runner, args)

  fun commandRequiresRoot(args: String): Boolean =
    when (args.substringBefore(' ')) {
      "apply", "charge", "misc", "throttle" -> true
      "clean" -> args == "clean ram" || args == "clean cache"
      else -> false
    }

  private suspend fun execute(
    context: Context,
    runner: ShellRunner,
    args: String,
    profile: Profile? = null,
    fromBoot: Boolean = false,
  ): Result =
    withContext(Dispatchers.IO) {
      operations.withLock {
        if (fromBoot && (profile == null || !ProfilePreferences.canReapply(context, profile.code))) {
          return@withLock Result(false, 125, "Boot reapply disabled or profile changed", logFile(context).absolutePath)
        }
        val mode = ShellAccess.modeOf(runner)
        val rootRequired = commandRequiresRoot(args)
        if (rootRequired && mode == ShellAccess.Mode.SHIZUKU) {
          return@withLock Result(false, 126, "Root access required", logFile(context).absolutePath)
        }
        val dir = ensure(context)
        val time = stamp()
        val staged = mode == ShellAccess.Mode.SHIZUKU
        val command =
          if (staged) {
            stagedCommand(context, args, profile, time)
          } else {
            if (profile != null) checkNotNull(TweakStore.writeConf(context, profile)) { "Could not write profile configuration" }
            checkNotNull(FeatureStore.writeConf(context)) { "Could not write feature configuration" }
            TweakCommands.run(dir.absolutePath, args, time)
          }
        val raw = runner.run(if (rootRequired) requireRoot(command) else command)
        finish(context, raw).also { result ->
          if (profile != null && result.ok && !fromBoot) ProfilePreferences.confirm(context, profile.code, mode)
          if (staged) record(context, "${mode.label} $args (exit ${result.exitCode})\n${result.output}")
        }
      }
    }

  private fun requireRoot(command: String): String =
    "if [ \"\$(id -u)\" != 0 ]; then printf 'Root access required\\n${TweakCommands.EXIT_MARKER}126\\n'; exit 126; fi\n$command"

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
      appendLine(writeStagedFile("custom/features.conf", FeatureStore.load(context).conf()))
      if (profile != null) {
        val path = "custom/${profile.name.lowercase(Locale.US)}.conf"
        appendLine(writeStagedFile(path, TweakStore.load(context, profile).conf()))
      }
      appendLine("cd \"\$MODDIR\" || exit 1")
      append(TweakCommands.run(".", args, time))
    }

  private fun stageAssets(context: Context, script: StringBuilder, dir: String) {
    for (name in context.assets.list("$ASSET_ROOT$dir").orEmpty()) {
      val child = "$dir/$name"
      val path = "$ASSET_ROOT$child"
      if (context.assets.list(path).orEmpty().isNotEmpty()) {
        script.appendLine("mkdir -p \"\$MODDIR\"${TweakCommands.quote(child)} || exit 1")
        stageAssets(context, script, child)
      } else {
        val content = context.assets.open(path).bufferedReader().use { it.readText() }
        script.appendLine(writeStagedFile(child.removePrefix("/"), content))
      }
    }
  }

  private fun writeStagedFile(name: String, content: String): String =
    "printf '%s' ${TweakCommands.quote(content)} > \"\$MODDIR/\"${TweakCommands.quote(name)} || exit 1"

  private fun finish(
    context: Context,
    raw: String,
  ): Result {
    val parsed = TweakCommands.parse(raw)
    val log = logFile(context)
    return Result(
      ok = parsed.exitCode == 0,
      exitCode = parsed.exitCode,
      output = parsed.output,
      logPath = log.absolutePath,
    )
  }

  private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

  private fun copyAssets(
    context: Context,
    assetPath: String,
    target: File,
  ) {
    val names = context.assets.list(assetPath).orEmpty()
    for (name in names) {
      val childAsset = "$assetPath/$name"
      val children = context.assets.list(childAsset).orEmpty()
      if (children.isNotEmpty()) {
        copyAssets(context, childAsset, File(target, name))
      } else {
        val out = File(target, name)
        out.parentFile?.mkdirs()
        context.assets.open(childAsset).use { input ->
          out.outputStream().use { output -> input.copyTo(output) }
        }
        out.setReadable(true, false)
        out.setExecutable(true, false)
      }
    }
  }
}
