package `in`.sms.tringapps.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.ShimmerBox
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.ui.VideoCard
import `in`.sms.tringapps.core.ui.VideoCardPlaceholder
import `in`.sms.tringapps.core.ui.VideoCardWidth
import `in`.sms.tringapps.feature.home.presentation.RailUiState
import kotlinx.coroutines.flow.Flow

private const val PLACEHOLDER_COUNT = 4

/**
 * A titled, horizontally scrolling shelf. Shows skeletons until the home screen has the
 * first page, then pages further items in as the user scrolls ([pagingFlow]).
 */
@Composable
fun VideoRail(
    rail: RailUiState,
    pagingFlow: Flow<PagingData<Video>>?,
    onVideoClick: (Video) -> Unit,
    onSeeAll: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag(HomeTestTags.rail(rail.playlistId))) {
        if (rail.title.isNotBlank()) {
            RailHeader(
                title = rail.title,
                onSeeAll = onSeeAll.takeIf { !rail.isLoading },
                seeAllTag = HomeTestTags.seeAll(rail.playlistId),
            )
        } else {
            ShimmerBox(
                Modifier
                    .padding(RailPadding)
                    .width(160.dp)
                    .height(20.dp),
                shape = MaterialTheme.shapes.small,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (rail.isLoading || pagingFlow == null) {
            PlaceholderRow()
        } else {
            PagedRow(pagingFlow, onVideoClick)
        }
    }
}

@Composable
private fun PlaceholderRow() {
    LazyRow(
        contentPadding = RailPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(PLACEHOLDER_COUNT) { VideoCardPlaceholder(Modifier.width(VideoCardWidth)) }
    }
}

@Composable
private fun PagedRow(pagingFlow: Flow<PagingData<Video>>, onVideoClick: (Video) -> Unit) {
    val items = pagingFlow.collectAsLazyPagingItems()
    if (items.itemCount == 0 && items.loadState.refresh is LoadState.Loading) {
        PlaceholderRow()
        return
    }
    LazyRow(
        contentPadding = RailPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Stable keys: the paging source drops duplicates across pages, so media ids are unique.
        items(count = items.itemCount, key = items.itemKey { it.mediaId }, contentType = { "video" }) { index ->
            items[index]?.let { video ->
                VideoCard(video = video, onClick = onVideoClick, modifier = Modifier.width(VideoCardWidth))
            }
        }
        when (items.loadState.append) {
            is LoadState.Loading -> item(contentType = "footer") { RailFooter { CircularProgressIndicator(color = AccentViolet) } }
            is LoadState.Error -> item(contentType = "footer") {
                RailFooter {
                    IconButton(onClick = items::retry) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.retry))
                    }
                }
            }
            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun RailFooter(content: @Composable () -> Unit) {
    Box(Modifier.size(width = 96.dp, height = 135.dp), contentAlignment = Alignment.Center) { content() }
}
