package `in`.sms.tringapps.feature.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.sms.tringapps.R
import `in`.sms.tringapps.core.designsystem.theme.AccentPink
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.designsystem.theme.BrandGradient
import `in`.sms.tringapps.core.designsystem.theme.Ink
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

const val BRAND_INTRO_TAG = "brand_intro"
private const val REVEAL_MS = 900
private const val HOLD_MS = 450L

/**
 * Short animated wordmark shown on cold start, right after the system splash: the
 * letters spread in while a light sweep runs across them, then [onFinished] fires.
 */
@Composable
fun BrandIntro(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnFinished = rememberUpdatedState(onFinished)
    val reveal = remember { Animatable(0f) }
    val sweep = remember { Animatable(-1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            listOf(
                async { reveal.animateTo(1f, tween(REVEAL_MS, easing = FastOutSlowInEasing)) },
                async { sweep.animateTo(2f, tween(REVEAL_MS + 300)) },
            ).awaitAll()
        }
        delay(HOLD_MS)
        currentOnFinished.value()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Ink)
            .testTag(BRAND_INTRO_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val progress = reveal.value
            Text(
                text = stringResource(R.string.brand_wordmark),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (2 + 8 * progress).sp,
                    brush = Brush.linearGradient(
                        colors = listOf(AccentViolet, Color.White, AccentPink),
                        start = Offset(sweep.value * 600f, 0f),
                        end = Offset(sweep.value * 600f + 400f, 120f),
                    ),
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = progress
                    val scale = 0.85f + 0.15f * progress
                    scaleX = scale
                    scaleY = scale
                },
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .height(3.dp)
                    .width((120 * progress).dp)
                    .background(BrandGradient, MaterialTheme.shapes.small),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.graphicsLayer { alpha = progress },
            )
        }
    }
}
