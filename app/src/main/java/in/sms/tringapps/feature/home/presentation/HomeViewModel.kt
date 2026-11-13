package `in`.sms.tringapps.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.usecase.GetHomeSectionsUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPagedPlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPlayablePlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetTrendingUseCase
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.usecase.ObserveContinueWatchingUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveMyListUseCase
import `in`.sms.tringapps.library.domain.usecase.ToggleMyListUseCase
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
    private val getHomeSections: GetHomeSectionsUseCase,
    private val getPlayablePlaylist: GetPlayablePlaylistUseCase,
    private val getPagedPlaylist: GetPagedPlaylistUseCase,
    private val getTrending: GetTrendingUseCase,
    observeContinueWatching: ObserveContinueWatchingUseCase,
    observeMyList: ObserveMyListUseCase,
    private val toggleMyListUseCase: ToggleMyListUseCase,
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
                _uiState.update { state ->
                    if (state is HomeUiState.Content) {
                        state.copy(continueWatching = latest.continueWatching, myList = latest.myList)
                    } else {
                        state
                    }
                }
            }
        }
        load()
    }

    fun retry() = load()

    /** Paged items of one rail; cached so scrolling position survives recomposition. */
    fun railPaging(playlistId: String): Flow<PagingData<Video>> =
        railPagingFlows.getOrPut(playlistId) { getPagedPlaylist(playlistId).cachedIn(viewModelScope) }

    fun toggleMyList(video: Video) {
        viewModelScope.launch { toggleMyListUseCase(video) }
    }

    private fun load() {
        loadJob?.cancel()
        railPagingFlows.clear()
        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val sections = getHomeSections().getOrElse {
                _uiState.value = HomeUiState.Error(it.toErrorKind())
                return@launch
            }
            if (sections.isEmpty()) {
                _uiState.value = HomeUiState.Error(HomeErrorKind.EMPTY)
                return@launch
            }

            val featuredId = sections.firstOrNull { it.featured }?.playlistId
            _uiState.value = HomeUiState.Content(
                hero = emptyList(),
                rails = sections.map { RailUiState(playlistId = it.playlistId, title = it.title) },
                continueWatching = library.continueWatching,
                myList = library.myList,
            )

            val trending = async {
                getTrending().getOrNull().orEmpty().also { videos ->
                    _uiState.update { (it as? HomeUiState.Content)?.copy(trending = videos) ?: it }
                }
            }
            val gate = Semaphore(MAX_PARALLEL_REQUESTS)
            val results = sections.map { section ->
                async {
                    gate.withPermit { getPlayablePlaylist(section.playlistId) }
                        .also { onRailResult(section, it, featuredId) }
                }
            }.awaitAll()
            trending.await()

            val content = _uiState.value as? HomeUiState.Content ?: return@launch
            if (content.rails.isEmpty() && content.trending.isEmpty()) {
                val firstFailure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                _uiState.value = HomeUiState.Error(
                    if (results.all { it.isFailure } && firstFailure != null) {
                        firstFailure.toErrorKind()
                    } else {
                        HomeErrorKind.EMPTY
                    },
                )
            }
        }
    }

    private fun onRailResult(section: Section, result: Result<Playlist>, featuredId: String?) {
        _uiState.update { state ->
            if (state !is HomeUiState.Content) return@update state
            val playlist = result.getOrNull()
            val rails = if (playlist == null || playlist.videos.isEmpty()) {
                state.rails.filterNot { it.playlistId == section.playlistId }
            } else {
                state.rails.map { rail ->
                    if (rail.playlistId != section.playlistId) {
                        rail
                    } else {
                        rail.copy(
                            title = rail.title.ifBlank { playlist.title },
                            videos = playlist.videos,
                            isLoading = false,
                        )
                    }
                }
            }
            state.copy(rails = rails, hero = state.hero.ifEmpty { pickHero(rails, featuredId) })
        }
    }

    /**
     * Hero items come from the featured playlist; if it has nothing playable (or there is
     * none), the first loaded rail is used instead. Waits for the featured rail first.
     */
    private fun pickHero(rails: List<RailUiState>, featuredId: String?) =
        rails.firstOrNull { it.playlistId == featuredId }
            ?.let { featured -> if (featured.isLoading) emptyList() else featured.videos.take(HERO_SIZE) }
            ?: rails.firstOrNull { !it.isLoading }?.videos?.take(HERO_SIZE).orEmpty()

    private fun Throwable.toErrorKind() =
        if (this is IOException) HomeErrorKind.NETWORK else HomeErrorKind.GENERIC

    companion object {
        const val MAX_PARALLEL_REQUESTS = 6
        const val HERO_SIZE = 5
    }
}
