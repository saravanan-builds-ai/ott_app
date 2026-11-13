package `in`.sms.tringapps.feature.home.presentation

import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem

const val HERO_SIZE = 5
const val TOP_TEN_SIZE = 10

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Error(val kind: HomeErrorKind) : HomeUiState

    /**
     * Rails appear as soon as the config loads and fill in as each playlist's first page
     * arrives. Rails whose playlist fails or has nothing playable are dropped.
     */
    data class Content(
        /** Source of the hero banner and the ranked Top 10; null until it is known. */
        val featured: FeaturedUiState?,
        val rails: List<RailUiState>,
        val continueWatching: List<ContinueWatchingItem> = emptyList(),
        val myList: List<Video> = emptyList(),
        /** Config titles of the special sections; null when the config doesn't declare them. */
        val continueWatchingTitle: String? = null,
        val myListTitle: String? = null,
        val isRefreshing: Boolean = false,
        /** Set when a pull-to-refresh failed; the previous content stays visible. */
        val refreshError: HomeErrorKind? = null,
    ) : HomeUiState {
        val hero: List<Video> get() = featured?.videos?.take(HERO_SIZE).orEmpty()
        val topTen: List<Video> get() = featured?.videos?.take(TOP_TEN_SIZE).orEmpty()
        val isHeroLoading: Boolean get() = featured == null && rails.any { it.isLoading }

        /** The featured playlist is shown as the ranked Top 10 instead of a plain rail. */
        val visibleRails: List<RailUiState> get() = rails.filter { it.playlistId != featured?.playlistId }
    }
}

enum class HomeErrorKind { NETWORK, GENERIC, EMPTY }

data class FeaturedUiState(
    val playlistId: String,
    val title: String,
    val videos: List<Video>,
)

data class RailUiState(
    val playlistId: String,
    val title: String,
    /** First page, used for visibility and the featured section. The rail itself is paged. */
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = true,
)
