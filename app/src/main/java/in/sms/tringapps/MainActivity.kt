package `in`.sms.tringapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import `in`.sms.tringapps.feature.intro.BrandIntro
import `in`.sms.tringapps.navigation.TringappsNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Always-dark UI: light system bar icons regardless of the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val coldStart = savedInstanceState == null
        setContent {
            TringappsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showIntro by rememberSaveable { mutableStateOf(coldStart) }
                    Box(Modifier.fillMaxSize()) {
                        // Home starts loading underneath while the intro plays.
                        TringappsNavHost()
                        AnimatedVisibility(visible = showIntro, exit = fadeOut(tween(durationMillis = 500))) {
                            BrandIntro(onFinished = { showIntro = false })
                        }
                    }
                }
            }
        }
    }
}
