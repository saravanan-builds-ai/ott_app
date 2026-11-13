package `in`.sms.tringapps.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.sms.tringapps.feature.home.presentation.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(
    onPlay: (mediaId: String) -> Unit,
    onOpenDetails: (mediaId: String) -> Unit,
    onSeeAll: (playlistId: String, title: String) -> Unit,
    onSearch: () -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onPlay = { onPlay(it.mediaId) },
            onOpenDetails = { onOpenDetails(it.mediaId) },
            onSeeAll = onSeeAll,
            onSearch = onSearch,
        )
    }
}
