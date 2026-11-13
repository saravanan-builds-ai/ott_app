package `in`.sms.tringapps.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.ui.VideoCard

private val LibraryCardWidth = 200.dp

/** One on-device library entry: a video and, for Continue Watching, its progress. */
data class LibraryEntry(val video: Video, val progress: Float? = null)

/** Rail backed by on-device data (Continue Watching, My List). */
@Composable
fun LibraryRail(
    title: String,
    entries: List<LibraryEntry>,
    onVideoClick: (Video) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag(testTag)) {
        RailHeader(title = title)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = RailPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.video.mediaId }) { entry ->
                VideoCard(
                    video = entry.video,
                    onClick = onVideoClick,
                    progress = entry.progress,
                    showDescription = false,
                    modifier = Modifier
                        .width(LibraryCardWidth)
                        .animateItem(),
                )
            }
        }
    }
}
