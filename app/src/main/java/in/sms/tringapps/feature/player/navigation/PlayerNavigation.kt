package `in`.sms.tringapps.feature.player.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.sms.tringapps.feature.player.presentation.PlayerRoute
import kotlinx.serialization.Serializable

/** `mediaId` must match [in.sms.tringapps.feature.player.presentation.PlayerViewModel.ARG_MEDIA_ID]. */
@Serializable
data class PlayerDestination(val mediaId: String)

fun NavController.navigateToPlayer(mediaId: String) {
    navigate(PlayerDestination(mediaId)) { launchSingleTop = true }
}

fun NavGraphBuilder.playerScreen(onBack: () -> Unit) {
    composable<PlayerDestination>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        PlayerRoute(onBack = onBack)
    }
}
