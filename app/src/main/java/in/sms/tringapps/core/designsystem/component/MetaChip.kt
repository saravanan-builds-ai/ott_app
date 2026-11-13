package `in`.sms.tringapps.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Small label such as an age rating ("16+") or a duration ("1h 32m"). */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val shape = RoundedCornerShape(6.dp)
    val base = if (outlined) {
        modifier.border(1.dp, Color.White.copy(alpha = 0.5f), shape)
    } else {
        modifier.background(Color.Black.copy(alpha = 0.6f), shape)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = base.padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
