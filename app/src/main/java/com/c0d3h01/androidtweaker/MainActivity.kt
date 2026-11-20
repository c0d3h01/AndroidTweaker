package com.c0d3h01.androidtweaker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import com.c0d3h01.androidtweaker.ui.TweakerTheme

/** A notification tap: the destination plus a nonce so repeated taps still route. */
data class ScreenRequest(
  val screen: String,
  val nonce: Long = System.nanoTime(),
)

class MainActivity : ComponentActivity() {
  private val screenRequest = mutableStateOf<ScreenRequest?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) screenRequest.value = intent.screenRequest()
    val model = ViewModelProvider(this)[TweakerViewModel::class.java]
    BatteryMonitorService.start(this)
    setContent {
      TweakerTheme {
        TweakerApp(
          model = model,
          screenRequest = screenRequest.value,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    screenRequest.value = intent.screenRequest()
  }

  private fun Intent.screenRequest(): ScreenRequest? = getStringExtra(EXTRA_SCREEN)?.let { ScreenRequest(it) }

  companion object {
    const val EXTRA_SCREEN = "com.c0d3h01.androidtweaker.extra.SCREEN"
    const val SCREEN_BATTERY = "battery"
    const val SCREEN_HOME = "home"
    const val SCREEN_DEVICE = "device"
    const val SCREEN_SETTINGS = "settings"
    const val SCREEN_PERFORMANCE = "performance"
    const val SCREEN_TWEAKS = "tweaks"
    const val SCREEN_HARDWARE = "hardware"
    const val SCREEN_LOGS = "logs"
    const val SCREEN_TOOLS = "tools"

    fun launchIntent(
      context: Context,
      screen: String,
    ): Intent = Intent(context, MainActivity::class.java).putExtra(EXTRA_SCREEN, screen)
  }
}
