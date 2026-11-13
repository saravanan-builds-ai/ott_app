package `in`.sms.tringapps.feature.player.presentation

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.sms.tringapps.R
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import `in`.sms.tringapps.testing.testVideo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun setScreen(
        state: PlayerUiState,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) = composeRule.setContent {
        TringappsTheme {
            PlayerScreen(state = state, onBack = onBack, onRetry = onRetry) { video, position ->
                Text("${video.mediaId}@$position", Modifier.testTag(FAKE_SURFACE))
            }
        }
    }

    @Test
    fun loading_showsProgressAndBackWorks() {
        var backs = 0
        setScreen(PlayerUiState.Loading, onBack = { backs++ })

        composeRule.onNodeWithTag(PlayerTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(PlayerTestTags.BACK).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun ready_rendersVideoSurfaceWithStartPosition() {
        setScreen(PlayerUiState.Ready(testVideo("v1"), startPositionMs = 1_500L))

        composeRule.onNodeWithTag(FAKE_SURFACE).assertIsDisplayed()
        composeRule.onNodeWithText("v1@1500").assertIsDisplayed()
    }

    @Test
    fun networkError_showsRetry() {
        var retries = 0
        setScreen(PlayerUiState.Error(PlayerError.NETWORK), onRetry = { retries++ })

        composeRule.onNodeWithText(string(R.string.player_error_network)).assertIsDisplayed()
        composeRule.onNodeWithTag(PlayerTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun playbackError_showsRetry() {
        setScreen(PlayerUiState.Error(PlayerError.PLAYBACK))

        composeRule.onNodeWithText(string(R.string.player_error_playback)).assertIsDisplayed()
        composeRule.onNodeWithTag(PlayerTestTags.RETRY).assertIsDisplayed()
    }

    @Test
    fun notPlayable_hasNoRetry() {
        setScreen(PlayerUiState.Error(PlayerError.NOT_PLAYABLE))

        composeRule.onNodeWithText(string(R.string.player_error_not_playable)).assertIsDisplayed()
        composeRule.onNodeWithTag(PlayerTestTags.RETRY).assertDoesNotExist()
    }

    private companion object {
        const val FAKE_SURFACE = "fake_surface"
    }
}
