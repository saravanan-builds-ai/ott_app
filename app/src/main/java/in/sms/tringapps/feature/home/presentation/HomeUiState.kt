package `in`.sms.tringapps.feature.home.presentation

import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Error(val kind: HomeErrorKind) : HomeUiState

    /**
     * Rails appear as soon as the config loads and fill in as each playlist's first page
     * arrives. Rails whose playlist fails or has nothing playable are dropped.
     */
    data class Content(
        val hero: List<Video>,
        val rails: List<RailUiState>,
        val trending: List<Video> = emptyList(),
        val continueWatching: List<ContinueWatchingItem> = emptyList(),
        val myList: List<Video> = emptyList(),
    ) : HomeUiState {
        val isHeroLoading: Boolean get() = hero.isEmpty() && rails.any { it.isLoading }
        fun isInMyList(mediaId: String): Boolean = myList.any { it.mediaId == mediaId }
    }
}

enum class HomeErrorKind { NETWORK, GENERIC, EMPTY }

data class RailUiState(
    val playlistId: String,
    val title: String,
    /** First page, used for visibility, the hero and search. The rail itself is paged. */
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = true,
)
