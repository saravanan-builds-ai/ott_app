package `in`.sms.tringapps.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.MetaChip
import `in`.sms.tringapps.core.designsystem.component.RemoteImage
import `in`.sms.tringapps.core.designsystem.component.ShimmerBox
import `in`.sms.tringapps.core.designsystem.format.durationText
import `in`.sms.tringapps.core.designsystem.theme.BrandGradient
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary

val VideoCardWidth: Dp = 240.dp

/**
 * 16:9 landscape card: thumbnail, title and a 2-line description.
 * When [progress] is set, a play badge and a progress bar are drawn (Continue Watching).
 */
@Composable
fun VideoCard(
    video: Video,
    onClick: (Video) -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    showDescription: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "cardScale")

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interactionSource, indication = null) { onClick(video) }
            .testTag(CatalogTestTags.videoCard(video.mediaId)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium),
        ) {
            RemoteImage(
                url = video.thumbnailUrl,
                contentDescription = stringResource(R.string.cd_thumbnail, video.title),
                modifier = Modifier.matchParentSize(),
            )
            video.ageRating?.let {
                MetaChip(it, Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            if (progress != null) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                }
                ProgressBar(progress, Modifier.align(Alignment.BottomCenter))
            } else {
                durationText(video.durationSeconds)?.let {
                    MetaChip(it, Modifier.align(Alignment.BottomEnd).padding(8.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showDescription && video.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.25f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(BrandGradient),
        )
    }
}

@Composable
fun VideoCardPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(10.dp))
        ShimmerBox(
            Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp),
            shape = MaterialTheme.shapes.small,
        )
        Spacer(Modifier.height(6.dp))
        ShimmerBox(
            Modifier
                .fillMaxWidth(0.9f)
                .height(10.dp),
            shape = MaterialTheme.shapes.small,
        )
    }
}

/** Fixed-width variant for horizontal rails. */
fun Modifier.railCardWidth(): Modifier = width(VideoCardWidth)
