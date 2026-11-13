package `in`.sms.tringapps.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import `in`.sms.tringapps.feature.home.navigation.HomeDestination
import `in`.sms.tringapps.feature.home.navigation.homeScreen
import `in`.sms.tringapps.feature.player.navigation.navigateToPlayer
import `in`.sms.tringapps.feature.player.navigation.playerScreen
import `in`.sms.tringapps.feature.search.navigation.navigateToSearch
import `in`.sms.tringapps.feature.search.navigation.searchScreen
import `in`.sms.tringapps.feature.seeall.navigation.navigateToSeeAll
import `in`.sms.tringapps.feature.seeall.navigation.seeAllScreen

@Composable
fun TringappsNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        homeScreen(
            onPlay = navController::navigateToPlayer,
            onSeeAll = navController::navigateToSeeAll,
            onSearch = navController::navigateToSearch,
        )
        seeAllScreen(onBack = back, onPlay = { navController.navigateToPlayer(it.mediaId) })
        searchScreen(onBack = back, onPlay = { navController.navigateToPlayer(it.mediaId) })
        playerScreen(onBack = back)
    }
}
