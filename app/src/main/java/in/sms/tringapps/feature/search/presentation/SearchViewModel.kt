package `in`.sms.tringapps.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.usecase.SearchCatalogUseCase
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SearchStatus { SUGGESTIONS, NOTHING_LOADED, RESULTS, NO_RESULTS }

data class SearchUiState(
    val status: SearchStatus = SearchStatus.SUGGESTIONS,
    /** The (debounced) query the [videos] belong to. */
    val query: String = "",
    val videos: List<Video> = emptyList(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchCatalog: SearchCatalogUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = query
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else DEBOUNCE_MS }
        .map(::search)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), search(""))

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun search(query: String): SearchUiState {
        if (query.isEmpty()) {
            val suggestions = searchCatalog.suggestions()
            return SearchUiState(
                status = if (suggestions.isEmpty()) SearchStatus.NOTHING_LOADED else SearchStatus.SUGGESTIONS,
                videos = suggestions,
            )
        }
        val results = searchCatalog(query)
        return SearchUiState(
            status = if (results.isEmpty()) SearchStatus.NO_RESULTS else SearchStatus.RESULTS,
            query = query,
            videos = results,
        )
    }

    companion object {
        const val DEBOUNCE_MS = 300L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
