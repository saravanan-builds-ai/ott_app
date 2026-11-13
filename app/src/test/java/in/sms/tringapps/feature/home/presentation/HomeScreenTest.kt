package `in`.sms.tringapps.feature.home.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import `in`.sms.tringapps.core.ui.CatalogTestTags
import `in`.sms.tringapps.feature.home.presentation.component.HomeTestTags
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.testing.testVideo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

    private val hero = testVideo("hero", title = "Almas pasajeras")
    private val second = testVideo("second", title = "Zootopia 2")
    private val card = testVideo("card", title = "Un perro andaluz")
    private val resume = testVideo("resume", title = "Resume me")
    private val content = HomeUiState.Content(
        featured = FeaturedUiState("F", "Destacados", listOf(hero, second)),
        continueWatching = listOf(ContinueWatchingItem(resume, 30_000, 60_000, 0)),
        continueWatchingTitle = "Continuar viendo",
        myListTitle = "Favoritos",
        rails = listOf(
            RailUiState("F", "Destacados", listOf(hero, second), isLoading = false),
            RailUiState("P1", "Novedades en cine", listOf(card), isLoading = false),
            RailUiState("P2", "Series", isLoading = true),
        ),
    )

    private fun setScreen(
        state: HomeUiState,
        onPlay: (Video) -> Unit = {},
        onOpenDetails: (Video) -> Unit = {},
        onRetry: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onSeeAll: (String, String) -> Unit = { _, _ -> },
        onSearch: () -> Unit = {},
    ) = composeRule.setContent {
        TringappsTheme {
            HomeScreen(
                state = state,
                onPlay = onPlay,
                onOpenDetails = onOpenDetails,
                onRetry = onRetry,
                onRefresh = onRefresh,
                onSeeAll = onSeeAll,
                onSearch = onSearch,
                autoAdvanceHero = false,
                heroPreviewEnabled = false,
            )
        }
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(HomeTestTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun loadingState_showsSkeleton() {
        setScreen(HomeUiState.Loading)
        composeRule.onNodeWithTag(HomeTestTags.LOADING).assertExists()
    }

    @Test
    fun errorState_showsMessageAndRetryInvokesCallback() {
        var retries = 0
        setScreen(HomeUiState.Error(HomeErrorKind.NETWORK), onRetry = { retries++ })

        composeRule.onNodeWithText(string(R.string.home_error_network)).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        setScreen(HomeUiState.Error(HomeErrorKind.EMPTY))
        composeRule.onNodeWithText(string(R.string.home_empty)).assertIsDisplayed()
    }

    @Test
    fun content_showsHeroSpecialSectionsTopTenAndRails() {
        setScreen(content)

        composeRule.onNodeWithTag(HomeTestTags.HERO).assertIsDisplayed()
        scrollTo(HomeTestTags.CONTINUE_WATCHING)
        composeRule.onNodeWithText(string(R.string.home_continue_watching)).assertIsDisplayed()
        scrollTo(HomeTestTags.TOP_TEN)
        composeRule.onNodeWithText("Destacados").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.rankedCard("hero")).assertIsDisplayed()
        scrollTo(CatalogTestTags.videoCard("card"))
        composeRule.onNodeWithText("Novedades en cine").assertIsDisplayed()
        // The featured playlist is not repeated as a plain rail.
        composeRule.onNodeWithTag(HomeTestTags.rail("F")).assertDoesNotExist()
    }

    @Test
    fun heroPlay_invokesOnPlayWithHeroVideo() {
        var played: Video? = null
        setScreen(content, onPlay = { played = it })

        composeRule.onNodeWithTag(HomeTestTags.HERO_PLAY).performClick()

        assertEquals(hero, played)
    }

    @Test
    fun heroInfo_opensDetails() {
        var opened: Video? = null
        setScreen(content, onOpenDetails = { opened = it })

        composeRule.onNodeWithTag(HomeTestTags.HERO_INFO).performClick()

        assertEquals(hero, opened)
    }

    @Test
    fun railCard_opensDetails() {
        var opened: Video? = null
        setScreen(content, onOpenDetails = { opened = it })

        scrollTo(CatalogTestTags.videoCard("card"))
        composeRule.onNodeWithTag(CatalogTestTags.videoCard("card")).performClick()

        assertEquals(card, opened)
    }

    @Test
    fun topTenCard_opensDetails() {
        var opened: Video? = null
        setScreen(content, onOpenDetails = { opened = it })

        scrollTo(HomeTestTags.rankedCard("second"))
        composeRule.onNodeWithTag(HomeTestTags.rankedCard("second")).performClick()

        assertEquals(second, opened)
    }

    @Test
    fun continueWatchingCard_playsDirectly() {
        var played: Video? = null
        setScreen(content, onPlay = { played = it })

        scrollTo(CatalogTestTags.videoCard("resume"))
        composeRule.onNodeWithTag(CatalogTestTags.videoCard("resume")).performClick()

        assertEquals(resume, played)
    }

    @Test
    fun seeAll_invokesCallbackWithRail() {
        var seeAll: Pair<String, String>? = null
        setScreen(content, onSeeAll = { id, title -> seeAll = id to title })

        scrollTo(HomeTestTags.seeAll("P1"))
        composeRule.onNodeWithTag(HomeTestTags.seeAll("P1")).performClick()

        assertEquals("P1" to "Novedades en cine", seeAll)
    }

    @Test
    fun topTenSeeAll_opensFeaturedPlaylist() {
        var seeAll: Pair<String, String>? = null
        setScreen(content, onSeeAll = { id, title -> seeAll = id to title })

        scrollTo(HomeTestTags.TOP_TEN_SEE_ALL)
        composeRule.onNodeWithTag(HomeTestTags.TOP_TEN_SEE_ALL).performClick()

        assertEquals("F" to "Destacados", seeAll)
    }

    @Test
    fun pullDown_requestsRefresh() {
        var refreshes = 0
        setScreen(content, onRefresh = { refreshes++ })

        composeRule.onNodeWithTag(HomeTestTags.LIST).performTouchInput { swipeDown(durationMillis = 400) }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun searchIcon_invokesOnSearch() {
        var searches = 0
        setScreen(content, onSearch = { searches++ })

        composeRule.onNodeWithTag(HomeTestTags.SEARCH).performClick()

        assertEquals(1, searches)
    }
}
