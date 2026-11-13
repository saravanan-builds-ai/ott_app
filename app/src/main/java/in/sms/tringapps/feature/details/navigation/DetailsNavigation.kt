package `in`.sms.tringapps.feature.details.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.sms.tringapps.feature.details.presentation.DetailsRoute
import kotlinx.serialization.Serializable

/**
 * Only the media id is passed; the screen resolves the video and its playback URL itself,
 * so nothing large or sensitive goes through the back stack / saved state.
 */
@Serializable
data class DetailsDestination(val mediaId: String)

fun NavController.navigateToDetails(mediaId: String) {
    navigate(DetailsDestination(mediaId)) { launchSingleTop = true }
}

fun NavGraphBuilder.detailsScreen(onBack: () -> Unit, onPlay: (mediaId: String) -> Unit) {
    composable<DetailsDestination>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
    ) {
        DetailsRoute(onBack = onBack, onPlay = { onPlay(it.mediaId) })
    }
}
