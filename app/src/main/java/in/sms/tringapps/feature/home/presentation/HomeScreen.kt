package `in`.sms.tringapps.feature.home.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.component.GradientButton
import `in`.sms.tringapps.core.designsystem.component.ShimmerBox
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.designsystem.theme.BrandGradient
import `in`.sms.tringapps.core.designsystem.theme.Ink
import `in`.sms.tringapps.core.designsystem.theme.InkElevated
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary
import `in`.sms.tringapps.feature.home.presentation.component.HeroCarousel
import `in`.sms.tringapps.feature.home.presentation.component.HeroPlaceholder
import `in`.sms.tringapps.feature.home.presentation.component.HomeTestTags
import `in`.sms.tringapps.feature.home.presentation.component.LibraryEntry
import `in`.sms.tringapps.feature.home.presentation.component.LibraryRail
import `in`.sms.tringapps.feature.home.presentation.component.TopTenRail
import `in`.sms.tringapps.feature.home.presentation.component.VideoRail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeRoute(
    onPlay: (Video) -> Unit,
    onOpenDetails: (Video) -> Unit,
    onSeeAll: (playlistId: String, title: String) -> Unit,
    onSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPlay = onPlay,
        onOpenDetails = onOpenDetails,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshErrorShown = viewModel::onRefreshErrorShown,
        onSeeAll = onSeeAll,
        onSearch = onSearch,
        railPaging = viewModel::railPaging,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlay: (Video) -> Unit,
    onOpenDetails: (Video) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onRefreshErrorShown: () -> Unit = {},
    onSeeAll: (playlistId: String, title: String) -> Unit = { _, _ -> },
    onSearch: () -> Unit = {},
    railPaging: (playlistId: String) -> Flow<PagingData<Video>>? = { null },
    autoAdvanceHero: Boolean = true,
    heroPreviewEnabled: Boolean = !LocalInspectionMode.current,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshError = (state as? HomeUiState.Content)?.refreshError
    val refreshFailedMessage = stringResource(R.string.home_refresh_failed)
    val currentOnRefreshErrorShown by rememberUpdatedState(onRefreshErrorShown)
    LaunchedEffect(refreshError) {
        if (refreshError != null) {
            snackbarHostState.showSnackbar(refreshFailedMessage)
            currentOnRefreshErrorShown()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        AnimatedContent(
            targetState = state.contentKey(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "homeState",
        ) { key ->
            when (key) {
                ContentKey.LOADING -> HomeLoading()
                ContentKey.ERROR -> HomeError(
                    kind = (state as? HomeUiState.Error)?.kind ?: HomeErrorKind.GENERIC,
                    onRetry = onRetry,
                )
                ContentKey.CONTENT -> (state as? HomeUiState.Content)?.let { content ->
                    HomeContent(
                        content = content,
                        listState = listState,
                        autoAdvanceHero = autoAdvanceHero,
                        heroPreviewEnabled = heroPreviewEnabled,
                        railPaging = railPaging,
                        onPlay = onPlay,
                        onOpenDetails = onOpenDetails,
                        onSeeAll = onSeeAll,
                        onRefresh = onRefresh,
                    )
                }
            }
        }

        HomeTopBar(listState = listState, solid = state !is HomeUiState.Content, onSearch = onSearch)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

private enum class ContentKey { LOADING, ERROR, CONTENT }

private fun HomeUiState.contentKey() = when (this) {
    HomeUiState.Loading -> ContentKey.LOADING
    is HomeUiState.Error -> ContentKey.ERROR
    is HomeUiState.Content -> ContentKey.CONTENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    listState: LazyListState,
    autoAdvanceHero: Boolean,
    heroPreviewEnabled: Boolean,
    railPaging: (String) -> Flow<PagingData<Video>>?,
    onPlay: (Video) -> Unit,
    onOpenDetails: (Video) -> Unit,
    onSeeAll: (String, String) -> Unit,
    onRefresh: () -> Unit,
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val heroVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = content.isRefreshing,
                containerColor = InkElevated,
                color = AccentViolet,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(HomeTestTags.LIST),
            contentPadding = PaddingValues(bottom = 32.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "hero", contentType = "hero") {
                val heroModifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(HERO_HEIGHT_FRACTION)
                    .heightIn(min = 420.dp)
                when {
                    content.hero.isNotEmpty() -> HeroCarousel(
                        videos = content.hero,
                        onPlay = onPlay,
                        onInfo = onOpenDetails,
                        autoAdvance = autoAdvanceHero,
                        previewEnabled = heroPreviewEnabled,
                        isVisible = heroVisible,
                        modifier = heroModifier,
                    )
                    content.isHeroLoading -> HeroPlaceholder(heroModifier)
                    else -> Spacer(
                        Modifier
                            .statusBarsPadding()
                            .height(56.dp),
                    )
                }
            }
            val continueWatchingTitle = content.continueWatchingTitle
            if (continueWatchingTitle != null && content.continueWatching.isNotEmpty()) {
                item(key = "continue_watching", contentType = "library") {
                    LibraryRail(
                        title = stringResource(R.string.home_continue_watching),
                        entries = content.continueWatching.map { LibraryEntry(it.video, it.progress) },
                        onVideoClick = onPlay,
                        testTag = HomeTestTags.CONTINUE_WATCHING,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            content.featured?.let { featured ->
                if (content.topTen.isNotEmpty()) {
                    item(key = "top_ten", contentType = "top_ten") {
                        TopTenRail(
                            sectionTitle = featured.title,
                            videos = content.topTen,
                            onVideoClick = onOpenDetails,
                            onSeeAll = { onSeeAll(featured.playlistId, featured.title) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            val myListTitle = content.myListTitle
            if (myListTitle != null && content.myList.isNotEmpty()) {
                item(key = "my_list", contentType = "library") {
                    LibraryRail(
                        title = stringResource(R.string.home_my_list),
                        entries = content.myList.map { LibraryEntry(it) },
                        onVideoClick = onOpenDetails,
                        testTag = HomeTestTags.MY_LIST,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(content.visibleRails, key = { it.playlistId }, contentType = { "rail" }) { rail ->
                val pagingFlow = remember(rail.playlistId, rail.isLoading, railPaging) {
                    if (rail.isLoading) null else railPaging(rail.playlistId) ?: flowOf(PagingData.from(rail.videos))
                }
                VideoRail(
                    rail = rail,
                    pagingFlow = pagingFlow,
                    onVideoClick = onOpenDetails,
                    onSeeAll = { onSeeAll(rail.playlistId, rail.title) },
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(listState: LazyListState, solid: Boolean, onSearch: () -> Unit) {
    val scrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }
    val backgroundAlpha by animateFloatAsState(if (solid || scrolled) 0.92f else 0f, label = "topBarAlpha")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Ink.copy(alpha = backgroundAlpha))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.brand_wordmark),
            style = MaterialTheme.typography.titleLarge.copy(
                brush = BrandGradient,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            ),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch, modifier = Modifier.testTag(HomeTestTags.SEARCH)) {
            Icon(
                Icons.Filled.Search,
                contentDescription = stringResource(R.string.home_search),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Column(
        Modifier
            .fillMaxSize()
            .testTag(HomeTestTags.LOADING),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(460.dp),
        )
        repeat(2) {
            VideoRail(
                rail = RailUiState(playlistId = "placeholder_$it", title = ""),
                pagingFlow = null,
                onVideoClick = {},
                onSeeAll = null,
            )
        }
    }
}

@Composable
private fun HomeError(kind: HomeErrorKind, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(HomeTestTags.ERROR),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .height(4.dp)
                .fillMaxWidth(0.2f)
                .background(BrandGradient, MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_error_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (kind) {
                    HomeErrorKind.NETWORK -> R.string.home_error_network
                    HomeErrorKind.GENERIC -> R.string.home_error_generic
                    HomeErrorKind.EMPTY -> R.string.home_empty
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(
            text = stringResource(R.string.retry),
            icon = Icons.Filled.Refresh,
            onClick = onRetry,
            modifier = Modifier.testTag(HomeTestTags.RETRY),
        )
    }
}

private const val HERO_HEIGHT_FRACTION = 0.72f
