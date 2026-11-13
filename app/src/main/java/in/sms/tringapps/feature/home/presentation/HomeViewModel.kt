package `in`.sms.tringapps.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.usecase.GetHomeContentUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPagedPlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPlayablePlaylistUseCase
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.usecase.ObserveContinueWatchingUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveMyListUseCase
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeContent: GetHomeContentUseCase,
    private val getPlayablePlaylist: GetPlayablePlaylistUseCase,
    private val getPagedPlaylist: GetPagedPlaylistUseCase,
    observeContinueWatching: ObserveContinueWatchingUseCase,
    observeMyList: ObserveMyListUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private data class Library(val continueWatching: List<ContinueWatchingItem>, val myList: List<Video>)

    private var library = Library(emptyList(), emptyList())
    private var loadJob: Job? = null
    private val railPagingFlows = mutableMapOf<String, Flow<PagingData<Video>>>()

    init {
        viewModelScope.launch {
            combine(observeContinueWatching(), observeMyList(), ::Library).collect { latest ->
                library = latest
                _uiState.update { (it as? HomeUiState.Content)?.withLibrary() ?: it }
            }
        }
        load(isRefresh = false)
    }

    fun retry() = load(isRefresh = false)

    /** Pull-to-refresh: refetches everything but keeps the current content until it's replaced. */
    fun refresh() = load(isRefresh = true)

    fun onRefreshErrorShown() {
        _uiState.update { (it as? HomeUiState.Content)?.copy(refreshError = null) ?: it }
    }

    /** Paged items of one rail; cached so scroll position and loaded pages survive recomposition. */
    fun railPaging(playlistId: String): Flow<PagingData<Video>> =
        railPagingFlows.getOrPut(playlistId) { getPagedPlaylist(playlistId).cachedIn(viewModelScope) }

    private fun load(isRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val previous = (_uiState.value as? HomeUiState.Content)?.takeIf { isRefresh }
            _uiState.value = previous?.copy(isRefreshing = true, refreshError = null) ?: HomeUiState.Loading

            val layout = getHomeContent(forceRefresh = isRefresh).getOrElse {
                fail(it.toErrorKind(), previous)
                return@launch
            }
            if (layout.playlists.isEmpty()) {
                fail(HomeErrorKind.EMPTY, previous)
                return@launch
            }

            railPagingFlows.clear()
            var content = HomeUiState.Content(
                featured = null,
                rails = layout.playlists.map { RailUiState(playlistId = it.playlistId, title = it.title) },
                continueWatchingTitle = layout.continueWatching?.title,
                myListTitle = layout.favorites?.title,
            )
            // First load renders progressively; a refresh swaps in the result at the end.
            fun publish() {
                if (previous == null) _uiState.value = content.withLibrary()
            }
            publish()

            val gate = Semaphore(MAX_PARALLEL_REQUESTS)
            val results = layout.playlists.map { section ->
                async {
                    gate.withPermit { getPlayablePlaylist(section.playlistId) }.also { result ->
                        content = content.withRailResult(section.playlistId, result, layout.featuredPlaylistId)
                        publish()
                    }
                }
            }.awaitAll()

            if (content.rails.isEmpty()) {
                val failure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                fail(
                    if (results.all { it.isFailure } && failure != null) failure.toErrorKind() else HomeErrorKind.EMPTY,
                    previous,
                )
            } else {
                _uiState.value = content.withLibrary()
            }
        }
    }

    private fun fail(kind: HomeErrorKind, previous: HomeUiState.Content?) {
        _uiState.value = previous?.copy(isRefreshing = false, refreshError = kind) ?: HomeUiState.Error(kind)
    }

    private fun HomeUiState.Content.withLibrary() =
        copy(continueWatching = library.continueWatching, myList = library.myList)

    private fun HomeUiState.Content.withRailResult(
        playlistId: String,
        result: Result<Playlist>,
        featuredId: String?,
    ): HomeUiState.Content {
        val playlist = result.getOrNull()
        val updated = if (playlist == null || playlist.videos.isEmpty()) {
            rails.filterNot { it.playlistId == playlistId }
        } else {
            rails.map { rail ->
                if (rail.playlistId != playlistId) {
                    rail
                } else {
                    rail.copy(title = rail.title.ifBlank { playlist.title }, videos = playlist.videos, isLoading = false)
                }
            }
        }
        return copy(rails = updated, featured = featured ?: pickFeatured(updated, featuredId))
    }

    /**
     * The config's featured playlist drives the hero and Top 10. If it is missing or has
     * nothing playable, the first loaded playlist in config order is used.
     */
    private fun pickFeatured(rails: List<RailUiState>, featuredId: String?): FeaturedUiState? {
        val preferred = rails.firstOrNull { it.playlistId == featuredId }
        if (preferred != null) {
            return if (preferred.isLoading) null else FeaturedUiState(preferred.playlistId, preferred.title, preferred.videos)
        }
        val fallback = rails.firstOrNull { !it.isLoading && it.videos.isNotEmpty() } ?: return null
        return FeaturedUiState(fallback.playlistId, fallback.title, fallback.videos)
    }

    private fun Throwable.toErrorKind() =
        if (this is IOException) HomeErrorKind.NETWORK else HomeErrorKind.GENERIC

    companion object {
        const val MAX_PARALLEL_REQUESTS = 10
    }
}
