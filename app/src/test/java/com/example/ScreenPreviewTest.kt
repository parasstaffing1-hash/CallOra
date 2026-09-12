package com.example

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the app's top-level screens to PNGs on the JVM via Robolectric + Roborazzi.
 * No emulator or device required. Run with:
 *   gradle :app:recordRoborazziDebug
 * Output lands in app/src/test/screenshots/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreenPreviewTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * AppDatabase seeds 4 clients and 2 calls from a RoomDatabase.Callback on a background
   * dispatcher, so the flows are briefly empty. Wait for the seed to land before capturing,
   * otherwise the screens render their empty states.
   */
  private fun seededViewModel(): MainViewModel {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)
    runBlocking {
      withTimeoutOrNull(20_000) { viewModel.allClients.first { it.isNotEmpty() } }
      withTimeoutOrNull(20_000) { viewModel.allRecordings.first { it.isNotEmpty() } }
    }
    return viewModel
  }

  private fun capture(name: String, content: @Composable () -> Unit) {
    composeTestRule.setContent { MyApplicationTheme { content() } }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
  }

  @Test
  fun home_screen() {
    val viewModel = seededViewModel()
    capture("screen_home") {
      HomeScreen(
        viewModel = viewModel,
        onNavigateToActiveRecord = {},
        onNavigateToCallDetail = {}
      )
    }
  }

  @Test
  fun clients_screen() {
    val viewModel = seededViewModel()
    capture("screen_clients") {
      ClientsScreen(viewModel = viewModel, onStartCallWithClient = {})
    }
  }

  @Test
  fun analytics_screen() {
    val viewModel = seededViewModel()
    capture("screen_analytics") {
      AnalyticsScreen(viewModel = viewModel)
    }
  }

  @Test
  fun settings_screen() {
    val viewModel = seededViewModel()
    capture("screen_settings") {
      SettingsScreen(viewModel = viewModel)
    }
  }
}
