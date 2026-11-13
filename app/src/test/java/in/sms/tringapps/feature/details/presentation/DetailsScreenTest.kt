package `in`.sms.tringapps.feature.details.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.sms.tringapps.R
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import `in`.sms.tringapps.testing.testVideo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private val video = testVideo("v1", title = "Almas pasajeras", durationSeconds = 5566)

    private fun setScreen(
        state: DetailsUiState,
        onBack: () -> Unit = {},
        onPlay: (Video) -> Unit = {},
        onToggleMyList: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) = composeRule.setContent {
        TringappsTheme {
            DetailsScreen(state, onBack, onPlay, onToggleMyList, onRetry)
        }
    }

    @Test
    fun content_showsTitleDescriptionDurationAndPlay() {
        var played: Video? = null
        setScreen(DetailsUiState.Content(video, isInMyList = false, resumeProgress = null), onPlay = { played = it })

        composeRule.onNodeWithText("Almas pasajeras").assertIsDisplayed()
        composeRule.onNodeWithText(video.description).assertExists()
        composeRule.onNodeWithText("1h 32m").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_play)).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailsTestTags.PLAY).performClick()

        assertEquals(video, played)
    }

    @Test
    fun startedTitle_showsResumeAndProgress() {
        setScreen(DetailsUiState.Content(video, isInMyList = false, resumeProgress = 0.4f))

        composeRule.onNodeWithText(string(R.string.details_resume)).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailsTestTags.PROGRESS).assertIsDisplayed()
    }

    @Test
    fun myListButton_reflectsStateAndToggles() {
        var toggles = 0
        setScreen(DetailsUiState.Content(video, isInMyList = true, resumeProgress = null), onToggleMyList = { toggles++ })

        composeRule.onNodeWithText(string(R.string.my_list_remove)).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailsTestTags.MY_LIST).performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun unplayableTitle_hasNoPlayButton() {
        setScreen(DetailsUiState.Content(testVideo("s", playable = false), isInMyList = false, resumeProgress = null))

        composeRule.onNodeWithTag(DetailsTestTags.PLAY).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.player_error_not_playable)).assertIsDisplayed()
    }

    @Test
    fun loading_showsProgress() {
        setScreen(DetailsUiState.Loading)
        composeRule.onNodeWithTag(DetailsTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun networkError_retries() {
        var retries = 0
        setScreen(DetailsUiState.Error(DetailsError.NETWORK), onRetry = { retries++ })

        composeRule.onNodeWithTag(DetailsTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun back_invokesCallback() {
        var backs = 0
        setScreen(DetailsUiState.Loading, onBack = { backs++ })

        composeRule.onNodeWithTag(DetailsTestTags.BACK).performClick()

        assertEquals(1, backs)
    }
}
