package com.c0d3h01.androidtweaker.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.c0d3h01.androidtweaker.data.ShellRunner
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

object ShizukuAccess {
  private const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"

  fun running(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

  fun granted(): Boolean = mode() != ShellAccess.Mode.NONE

  internal fun mode(): ShellAccess.Mode =
    runCatching {
      if (!running() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
        ShellAccess.Mode.NONE
      } else {
        modeForUid(Shizuku.getUid())
      }
    }.getOrDefault(ShellAccess.Mode.NONE)

  internal fun modeForUid(uid: Int): ShellAccess.Mode =
    when (uid) {
      0 -> ShellAccess.Mode.SHIZUKU_ROOT
      2000 -> ShellAccess.Mode.SHIZUKU
      else -> ShellAccess.Mode.NONE
    }

  fun installed(context: Context): Boolean =
    runCatching {
      context.packageManager.getPackageInfo(MANAGER_PACKAGE, 0)
      true
    }.getOrDefault(false)

  private var listener: Shizuku.OnRequestPermissionResultListener? = null
  private var nextRequestCode = 0x5A1

  /** Requests permission from the foreground. [onResult] runs on the main thread. */
  fun request(onResult: (Boolean) -> Unit) {
    if (listener != null) return
    val alreadyGranted = granted()
    if (alreadyGranted || !running()) {
      onResult(alreadyGranted)
      return
    }

    val handler = Handler(Looper.getMainLooper())
    val requestCode = nextRequestCode++
    lateinit var request: Shizuku.OnRequestPermissionResultListener
    lateinit var timeout: Runnable
    fun finish(granted: Boolean) {
      if (listener !== request) return
      listener = null
      handler.removeCallbacks(timeout)
      Shizuku.removeRequestPermissionResultListener(request)
      onResult(granted)
    }
    request = Shizuku.OnRequestPermissionResultListener { code, result ->
      if (code == requestCode) finish(result == PackageManager.PERMISSION_GRANTED && granted())
    }
    timeout = Runnable { finish(false) }
    listener = request
    handler.postDelayed(timeout, 45_000)
    runCatching {
      Shizuku.addRequestPermissionResultListener(request, handler)
      Shizuku.requestPermission(requestCode)
    }.onFailure { finish(false) }
  }

  private val rootRunner = makeRunner(ShellAccess.Mode.SHIZUKU_ROOT)
  private val adbRunner = makeRunner(ShellAccess.Mode.SHIZUKU)

  fun runnerOrNull(): ShellRunner? = runnerFor(mode())

  internal fun runnerFor(mode: ShellAccess.Mode): ShellRunner? =
    when (mode) {
      ShellAccess.Mode.SHIZUKU_ROOT -> rootRunner
      ShellAccess.Mode.SHIZUKU -> adbRunner
      else -> null
    }

  private fun makeRunner(expected: ShellAccess.Mode): ShellRunner =
    ShellRunner { command ->
      check(mode() == expected) { "Shizuku connection changed" }
      shizukuRun(command)
    }

  fun openManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(MANAGER_PACKAGE)
    val intent =
      launch
        ?: Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://play.google.com/store/apps/details?id=$MANAGER_PACKAGE"),
        )
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  }

  // shortcut: the pinned Shizuku API exposes the legacy process bridge privately, migrate to a UserService before upgrading to API 14.
  private val newProcess: java.lang.reflect.Method by lazy {
    Shizuku::class.java
      .getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
      .apply { isAccessible = true }
  }

  private fun shizukuRun(cmd: String): String {
    val process =
      try {
        newProcess.invoke(null, arrayOf("sh", "-c", cmd), null, null) as ShizukuRemoteProcess
      } catch (e: InvocationTargetException) {
        throw IllegalStateException("Shizuku could not start a shell", e.cause ?: e)
      }
    val stdout = FutureTask { process.inputStream.bufferedReader().use { it.readText() } }
    val stderr = FutureTask { process.errorStream.bufferedReader().use { it.readText() } }
    Thread(stdout, "AndroidTweaker-Shizuku-out").apply { isDaemon = true }.start()
    Thread(stderr, "AndroidTweaker-Shizuku-err").apply { isDaemon = true }.start()
    try {
      check(process.waitForTimeout(30, TimeUnit.SECONDS)) { "Shizuku shell timed out" }
      val out = stdout.get(1, TimeUnit.SECONDS)
      val err = stderr.get(1, TimeUnit.SECONDS)
      val code = process.exitValue()
      return buildString {
        append(out)
        if (err.isNotEmpty()) {
          if (isNotEmpty()) append('\n')
          append(err)
        }
        if (code != 0) append("\n[exit $code]")
      }
    } finally {
      stdout.cancel(true)
      stderr.cancel(true)
      runCatching { if (process.alive()) process.destroy() }
    }
  }
}

object ShellAccess {
  enum class Mode(
    val label: String,
    val canApplyProfiles: Boolean = false,
  ) {
    ROOT("Root", true),
    SHIZUKU_ROOT("Shizuku (root)", true),
    SHIZUKU("Shizuku (ADB)"),
    NONE("No shell"),
  }

  fun mode(): Mode = if (RootShell.runnerOrNull() != null) Mode.ROOT else ShizukuAccess.mode()

  fun modeOf(runner: ShellRunner): Mode =
    when {
      runner is SuProcessRunner -> Mode.ROOT
      runner === ShizukuAccess.runnerFor(Mode.SHIZUKU_ROOT) -> Mode.SHIZUKU_ROOT
      runner === ShizukuAccess.runnerFor(Mode.SHIZUKU) -> Mode.SHIZUKU
      else -> Mode.NONE
    }

  fun runnerOrNull(requireRoot: Boolean = false): ShellRunner? {
    RootShell.runnerOrNull()?.let { return it }
    val mode = ShizukuAccess.mode()
    return if (!requireRoot || mode.canApplyProfiles) ShizukuAccess.runnerFor(mode) else null
  }

  fun ensure(requireRoot: Boolean = false): ShellRunner =
    runnerOrNull(requireRoot) ?: error(if (requireRoot) "Connect root to continue" else "Connect root or Shizuku to continue")
}
