package com.c0d3h01.androidtweaker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GpuRendererTest {
  @Test
  fun emulatorTranslatorReportsSoftwareRenderer() {
    val line =
      "Google (Google Inc. (Google)), Android Emulator OpenGL ES Translator (ANGLE " +
        "(Google, Vulkan 1.3.0 (SwiftShader Device (LLVM 10.0.0)), SwiftShader driver-5.0.0)), " +
        "OpenGL ES 3.1 GL_EXT_debug_marker GL_APPLE_texture_format_BGRA8888"
    assertEquals("SwiftShader (software)", SystemReader.gpuRendererName(line))
  }

  @Test
  fun adrenoDropsTrademarkSuffix() {
    val line = "Qualcomm, Adreno (TM) 740, OpenGL ES 3.2 V@0615.0"
    assertEquals("Adreno 740", SystemReader.gpuRendererName(line))
  }

  @Test
  fun maliKeepsModelAndCores() {
    val line = "ARM, Mali-G78 MP14, OpenGL ES 3.2 v1.r38p1"
    assertEquals("Mali-G78 MP14", SystemReader.gpuRendererName(line))
  }

  @Test
  fun emulatorReportsHostGpu() {
    val line =
      "Apple (Apple), Android Emulator OpenGL ES Translator (Apple M5), OpenGL ES 3.0 (4.1 Metal - 91.7)"
    assertEquals("Apple M5 (emulator)", SystemReader.gpuRendererName(line))
  }

  @Test
  fun extensionAppleIsNotARenderer() {
    val line = "Google, Android Emulator OpenGL ES Translator (ANGLE), OpenGL ES 3.1 GL_APPLE_framebuffer"
    assertEquals("Android Emulator", SystemReader.gpuRendererName(line))
  }
}
