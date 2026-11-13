package `in`.sms.tringapps.feature.seeall.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.sms.tringapps.feature.seeall.presentation.SeeAllRoute
import kotlinx.serialization.Serializable

/** Property names must match [in.sms.tringapps.feature.seeall.presentation.SeeAllViewModel] args. */
@Serializable
data class SeeAllDestination(val playlistId: String, val title: String)

fun NavController.navigateToSeeAll(playlistId: String, title: String) {
    navigate(SeeAllDestination(playlistId, title)) { launchSingleTop = true }
}

fun NavGraphBuilder.seeAllScreen(onBack: () -> Unit, onOpenDetails: (mediaId: String) -> Unit) {
    composable<SeeAllDestination>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        SeeAllRoute(onBack = onBack, onOpenDetails = { onOpenDetails(it.mediaId) })
    }
}
