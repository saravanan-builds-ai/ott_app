package `in`.sms.tringapps.feature.home.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.RemoteImage
import `in`.sms.tringapps.core.designsystem.theme.BrandGradient
import androidx.compose.material3.Text

private val PosterWidth = 124.dp
private val PosterHeight = 176.dp

/** Netflix-style ranked Top 10: oversized outlined numerals tucked behind tall posters. */
@Composable
fun TrendingRail(
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag(HomeTestTags.TRENDING)) {
        RailHeader(title = stringResource(R.string.home_trending_now))
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = RailPadding,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(videos, key = { _, video -> video.mediaId }) { index, video ->
                RankedCard(rank = index + 1, video = video, onClick = onVideoClick)
            }
        }
    }
}

@Composable
private fun RankedCard(rank: Int, video: Video, onClick: (Video) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "rankScale")
    val description = stringResource(R.string.cd_rank, rank) + ", " + video.title
    val numeralWidth = if (rank >= 10) 112.dp else 72.dp

    Box(
        modifier = Modifier
            .width(numeralWidth + PosterWidth - 18.dp)
            .height(PosterHeight + 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) { onClick(video) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag(HomeTestTags.rankedCard(video.mediaId)),
    ) {
        Text(
            text = rank.toString(),
            style = TextStyle(
                fontSize = 150.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-12).sp,
                brush = BrandGradient,
                drawStyle = Stroke(width = 7f, join = StrokeJoin.Round),
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = 34.dp)
                .clearAndSetSemantics { },
        )
        RemoteImage(
            url = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(PosterWidth)
                .height(PosterHeight)
                .shadow(12.dp, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}
