package com.c0d3h01.androidtweaker.engine

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Existing asset/runtime layout and versioned extraction. */
object EngineFiles {
  internal const val ASSET_ROOT = "tweaker"
  private const val LOG_NAME = "tweaks.log"
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

  fun ensure(context: Context): File {
    val dir = directory(context)
    val marker = File(dir, ".engine-${TweakEngine.ENGINE_VERSION}")
    if (!marker.isFile || !ready(context)) {
      dir.mkdirs()
      copyAssets(context, ASSET_ROOT, dir)
      marker.writeText(TweakEngine.ENGINE_VERSION)
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

  fun stamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

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
