package `in`.sms.tringapps.feature.search.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import `in`.sms.tringapps.core.ui.CatalogTestTags
import `in`.sms.tringapps.testing.testVideo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun setScreen(
        state: SearchUiState,
        query: String = "",
        onQueryChange: (String) -> Unit = {},
        onPlay: (Video) -> Unit = {},
    ) = composeRule.setContent {
        TringappsTheme {
            SearchScreen(
                query = query,
                state = state,
                onQueryChange = onQueryChange,
                onBack = {},
                onPlay = onPlay,
                autoFocus = false,
            )
        }
    }

    @Test
    fun suggestions_areShownWithHeader() {
        setScreen(SearchUiState(SearchStatus.SUGGESTIONS, videos = listOf(testVideo("a"))))

        composeRule.onNodeWithText(context.getString(R.string.search_top_picks)).assertIsDisplayed()
        composeRule.onNodeWithTag(CatalogTestTags.videoCard("a")).assertIsDisplayed()
    }

    @Test
    fun typing_forwardsQuery() {
        var typed = ""
        setScreen(SearchUiState(), onQueryChange = { typed = it })

        composeRule.onNodeWithTag(SearchTestTags.FIELD).performTextInput("perro")

        assertEquals("perro", typed)
    }

    @Test
    fun clear_resetsQuery() {
        var typed = "x"
        setScreen(SearchUiState(), query = "perro", onQueryChange = { typed = it })

        composeRule.onNodeWithTag(SearchTestTags.CLEAR).performClick()

        assertEquals("", typed)
    }

    @Test
    fun noResults_showsMessageWithQuery() {
        setScreen(SearchUiState(SearchStatus.NO_RESULTS, query = "zzz"), query = "zzz")

        composeRule.onNodeWithText(context.getString(R.string.search_no_results, "zzz")).assertIsDisplayed()
    }

    @Test
    fun nothingLoaded_showsHint() {
        setScreen(SearchUiState(SearchStatus.NOTHING_LOADED))
        composeRule.onNodeWithText(context.getString(R.string.search_nothing_loaded)).assertIsDisplayed()
    }

    @Test
    fun resultClick_opensDetailsAndPlays() {
        var played: Video? = null
        val video = testVideo("a")
        setScreen(SearchUiState(SearchStatus.RESULTS, "a", listOf(video)), query = "a", onPlay = { played = it })

        composeRule.onNodeWithTag(CatalogTestTags.videoCard("a")).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CatalogTestTags.DETAILS_PLAY).performClick()

        assertEquals(video, played)
    }
}
