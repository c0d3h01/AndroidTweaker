package com.c0d3h01.androidtweaker.util

import com.c0d3h01.androidtweaker.data.ShellRunner
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Serializes commands through one shell for the lifetime of a root grant. */
class SuProcessRunner(
  private val command: List<String> = listOf("su"),
  private val timeoutMillis: Long = 30_000,
) : ShellRunner, Closeable {
  private val executor = Executors.newSingleThreadExecutor { work ->
    Thread(work, "AndroidTweaker-su").apply { isDaemon = true }
  }
  @Volatile private var process: Process? = null
  private var reader: BufferedReader? = null
  private var closed = false

  val isActive: Boolean
    get() = process?.isAlive == true

  @Synchronized
  override fun run(cmd: String): String {
    check(!closed) { "Root session closed" }
    val started = process ?: try {
      ProcessBuilder(command).redirectErrorStream(true).start().also {
        process = it
        reader = it.inputStream.bufferedReader()
      }
    } catch (e: IOException) {
      close()
      throw IllegalStateException("su not available", e)
    }
    val out = checkNotNull(reader)
    val response = executor.submit<String> { exchange(started, out, cmd) }
    return try {
      response.get(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
      throw fail("Root shell timed out", e)
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      throw fail("Root shell interrupted", e)
    } catch (e: ExecutionException) {
      throw fail("Root shell disconnected", e.cause ?: e)
    }
  }

  private fun exchange(process: Process, out: BufferedReader, cmd: String): String {
    val end = "__AT_${UUID.randomUUID()}__"
    val script = "'" + cmd.replace("'", "'\\''") + "'"
    process.outputStream.write("(eval $script </dev/null); printf '%s\\n' '$end'\n".toByteArray())
    process.outputStream.flush()
    return buildString {
      while (true) {
        val line = out.readLine() ?: throw IOException("su exited before returning output")
        if (line.endsWith(end)) {
          append(line.removeSuffix(end))
          return@buildString
        }
        appendLine(line)
      }
    }
  }

  private fun fail(message: String, cause: Throwable): IllegalStateException {
    close()
    return IllegalStateException(message, cause)
  }

  @Synchronized
  override fun close() {
    closed = true
    val running = process
    process = null
    reader = null
    executor.shutdownNow()
    running?.destroyForcibly()
  }
}
