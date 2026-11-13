package `in`.sms.tringapps.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import `in`.sms.tringapps.core.designsystem.theme.InkElevated
import `in`.sms.tringapps.core.designsystem.theme.InkRaised

/** Placeholder block with a moving highlight, used while content loads. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    Box(
        modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(InkRaised, InkElevated, InkRaised),
                start = Offset(x = progress * 600f, y = 0f),
                end = Offset(x = progress * 600f + 600f, y = 300f),
            ),
            shape = shape,
        ),
    )
}
