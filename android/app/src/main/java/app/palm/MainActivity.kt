package app.palm

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneTheme
import app.palm.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity: required for AppCompatDelegate.setApplicationLocales with Compose (D-06)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // must precede super.onCreate
        // Dark-only app: light bar icons regardless of the system theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Held until the first profile read decides the start route (D-02)
        splash.setKeepOnScreenCondition { vm.start.value == null }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate().alpha(0f).setDuration(NocturneDurations.Quick.toLong())
                .withEndAction { provider.remove() }.start()
        }
        setContent {
            NocturneTheme { AppNavDisplay(start = vm.start.collectAsStateWithLifecycle().value) }
        }
    }
}
