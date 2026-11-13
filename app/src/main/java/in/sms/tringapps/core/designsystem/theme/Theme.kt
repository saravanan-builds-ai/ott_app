package `in`.sms.tringapps.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TringappsColorScheme = darkColorScheme(
    primary = AccentViolet,
    onPrimary = Color.White,
    secondary = AccentPink,
    onSecondary = Color.White,
    tertiary = AccentGold,
    background = Ink,
    onBackground = TextPrimary,
    surface = InkRaised,
    onSurface = TextPrimary,
    surfaceVariant = InkElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = InkRaised,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkElevated,
    outline = Outline,
    error = ErrorRed,
)

private val TringappsShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

/** Premium, always-dark theme used across the app. */
@Composable
fun TringappsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TringappsColorScheme,
        typography = TringappsTypography,
        shapes = TringappsShapes,
        content = content,
    )
}
