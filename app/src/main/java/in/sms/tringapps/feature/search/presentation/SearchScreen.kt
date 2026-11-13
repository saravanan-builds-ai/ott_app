package `in`.sms.tringapps.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.theme.AccentViolet
import `in`.sms.tringapps.core.designsystem.theme.Ink
import `in`.sms.tringapps.core.designsystem.theme.InkElevated
import `in`.sms.tringapps.core.designsystem.theme.TextSecondary
import `in`.sms.tringapps.core.ui.BackTopBar
import `in`.sms.tringapps.core.ui.VideoCard
import `in`.sms.tringapps.core.ui.VideoDetailsSheet

object SearchTestTags {
    const val FIELD = "search_field"
    const val CLEAR = "search_clear"
    const val GRID = "search_grid"
    const val MESSAGE = "search_message"
}

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onPlay: (Video) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The text field owns its text so typing never waits on the debounced results.
    var query by rememberSaveable { mutableStateOf("") }
    SearchScreen(
        query = query,
        state = state,
        onQueryChange = {
            query = it
            viewModel.onQueryChange(it)
        },
        onBack = onBack,
        onPlay = onPlay,
    )
}

@Composable
fun SearchScreen(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = !LocalInspectionMode.current,
) {
    var selected by remember { mutableStateOf<Video?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Ink)
            .imePadding(),
    ) {
        BackTopBar(onBack = onBack) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint), color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag(SearchTestTags.CLEAR)) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InkElevated,
                    unfocusedContainerColor = InkElevated,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AccentViolet,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .focusRequester(focusRequester)
                    .testTag(SearchTestTags.FIELD),
            )
        }

        when (state.status) {
            SearchStatus.NOTHING_LOADED -> Message(stringResource(R.string.search_nothing_loaded))
            SearchStatus.NO_RESULTS -> Message(stringResource(R.string.search_no_results, state.query))
            SearchStatus.SUGGESTIONS, SearchStatus.RESULTS -> ResultsGrid(
                title = stringResource(
                    if (state.status == SearchStatus.RESULTS) R.string.search_results else R.string.search_top_picks,
                ),
                videos = state.videos,
                onVideoClick = {
                    focusManager.clearFocus()
                    selected = it
                },
            )
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

@Composable
private fun ResultsGrid(title: String, videos: List<Video>, onVideoClick: (Video) -> Unit) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + bottomInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.GRID),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        items(videos, key = { it.mediaId }) { video ->
            VideoCard(video = video, onClick = onVideoClick, showDescription = false)
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag(SearchTestTags.MESSAGE),
    )
}
