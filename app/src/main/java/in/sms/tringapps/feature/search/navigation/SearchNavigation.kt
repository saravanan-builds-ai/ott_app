package `in`.sms.tringapps.feature.search.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.sms.tringapps.feature.search.presentation.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavController.navigateToSearch() {
    navigate(SearchDestination) { launchSingleTop = true }
}

fun NavGraphBuilder.searchScreen(onBack: () -> Unit, onOpenDetails: (mediaId: String) -> Unit) {
    composable<SearchDestination>(enterTransition = { fadeIn() }, popExitTransition = { fadeOut() }) {
        SearchRoute(onBack = onBack, onOpenDetails = { onOpenDetails(it.mediaId) })
    }
}
