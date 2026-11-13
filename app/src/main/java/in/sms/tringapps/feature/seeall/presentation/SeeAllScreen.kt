package `in`.sms.tringapps.feature.seeall.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.GradientButton
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.designsystem.theme.Ink
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary
import `in`.sms.tringapps.core.ui.BackTopBar
import `in`.sms.tringapps.core.ui.VideoCard
import `in`.sms.tringapps.core.ui.VideoCardPlaceholder
import `in`.sms.tringapps.core.ui.VideoDetailsSheet
import kotlinx.coroutines.flow.Flow

object SeeAllTestTags {
    const val GRID = "see_all_grid"
    const val LOADING = "see_all_loading"
    const val ERROR = "see_all_error"
    const val RETRY = "see_all_retry"
    const val EMPTY = "see_all_empty"
    const val APPEND_LOADING = "see_all_append_loading"
    const val APPEND_RETRY = "see_all_append_retry"
}

@Composable
fun SeeAllRoute(
    onBack: () -> Unit,
    onPlay: (Video) -> Unit,
    viewModel: SeeAllViewModel = hiltViewModel(),
) {
    SeeAllScreen(title = viewModel.title, videos = viewModel.videos, onBack = onBack, onPlay = onPlay)
}

/** Paged vertical grid of a whole playlist. */
@Composable
fun SeeAllScreen(
    title: String,
    videos: Flow<PagingData<Video>>,
    onBack: () -> Unit,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = videos.collectAsLazyPagingItems()
    var selected by remember { mutableStateOf<Video?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        BackTopBar(onBack = onBack) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val refresh = items.loadState.refresh
        when {
            items.itemCount == 0 && refresh is LoadState.Loading -> LoadingGrid()
            items.itemCount == 0 && refresh is LoadState.Error -> Message(
                text = stringResource(R.string.home_error_network),
                tag = SeeAllTestTags.ERROR,
                onRetry = items::retry,
            )
            items.itemCount == 0 && refresh is LoadState.NotLoading && items.loadState.append.endOfPaginationReached ->
                Message(text = stringResource(R.string.see_all_empty), tag = SeeAllTestTags.EMPTY)
            else -> VideoGrid(items, onVideoClick = { selected = it })
        }
    }

    selected?.let { video ->
        VideoDetailsSheet(
            video = video,
            onPlay = {
                selected = null
                onPlay(it)
            },
            onDismiss = { selected = null },
        )
    }
}

private val GridMinCell = 170.dp

private fun gridPadding(bottom: androidx.compose.ui.unit.Dp) =
    PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + bottom)

@Composable
private fun VideoGrid(items: LazyPagingItems<Video>, onVideoClick: (Video) -> Unit) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridMinCell),
        contentPadding = gridPadding(bottomInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag(SeeAllTestTags.GRID),
    ) {
        items(count = items.itemCount, contentType = { "video" }) { index ->
            items[index]?.let { VideoCard(video = it, onClick = onVideoClick) }
        }
        when (items.loadState.append) {
            is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentViolet, modifier = Modifier.testTag(SeeAllTestTags.APPEND_LOADING))
                }
            }
            is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.see_all_load_more_failed), color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = stringResource(R.string.retry),
                        icon = Icons.Filled.Refresh,
                        onClick = items::retry,
                        modifier = Modifier.testTag(SeeAllTestTags.APPEND_RETRY),
                    )
                }
            }
            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun LoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridMinCell),
        contentPadding = gridPadding(0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxSize()
            .testTag(SeeAllTestTags.LOADING),
    ) {
        items(8) { VideoCardPlaceholder() }
    }
}

@Composable
private fun Message(text: String, tag: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = stringResource(R.string.retry),
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
                modifier = Modifier.testTag(SeeAllTestTags.RETRY),
            )
        }
    }
}
