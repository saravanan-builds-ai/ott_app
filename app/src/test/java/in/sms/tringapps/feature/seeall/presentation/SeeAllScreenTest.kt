package `in`.sms.tringapps.feature.seeall.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import `in`.sms.tringapps.core.ui.BACK_BUTTON_TAG
import `in`.sms.tringapps.core.ui.CatalogTestTags
import `in`.sms.tringapps.testing.testVideo
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeeAllScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        videos: Flow<PagingData<Video>>,
        onBack: () -> Unit = {},
        onOpenDetails: (Video) -> Unit = {},
    ) = composeRule.setContent {
        TringappsTheme {
            SeeAllScreen(title = "Novedades en cine", videos = videos, onBack = onBack, onOpenDetails = onOpenDetails)
        }
    }

    /** Paging delivers data a few frames after composition; wait for it. */
    private fun awaitTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private val notLoading = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    @Test
    fun showsTitleAndGridItems() {
        setScreen(flowOf(PagingData.from(listOf(testVideo("a"), testVideo("b")))))
        awaitTag(CatalogTestTags.videoCard("a"))

        composeRule.onNodeWithText("Novedades en cine").assertIsDisplayed()
        composeRule.onNodeWithTag(SeeAllTestTags.GRID).assertIsDisplayed()
        composeRule.onNodeWithTag(CatalogTestTags.videoCard("a")).assertIsDisplayed()
    }

    @Test
    fun emptyPlaylist_showsEmptyMessage() {
        setScreen(flowOf(PagingData.empty(notLoading)))
        awaitTag(SeeAllTestTags.EMPTY)
        composeRule.onNodeWithTag(SeeAllTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun refreshError_showsRetry() {
        val error = notLoading.copy(refresh = LoadState.Error(IOException()))
        setScreen(flowOf(PagingData.empty(error)))
        awaitTag(SeeAllTestTags.ERROR)

        composeRule.onNodeWithTag(SeeAllTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(SeeAllTestTags.RETRY).assertIsDisplayed()
    }

    @Test
    fun refreshLoading_showsSkeletonGrid() {
        setScreen(flowOf(PagingData.empty(notLoading.copy(refresh = LoadState.Loading))))
        composeRule.onNodeWithTag(SeeAllTestTags.LOADING).assertExists()
    }

    @Test
    fun appendLoading_showsFooterSpinner() {
        val states = notLoading.copy(append = LoadState.Loading)
        setScreen(flowOf(PagingData.from(listOf(testVideo("a")), sourceLoadStates = states)))
        awaitTag(SeeAllTestTags.APPEND_LOADING)
        composeRule.onNodeWithTag(SeeAllTestTags.APPEND_LOADING).assertExists()
    }

    @Test
    fun itemClick_opensDetails() {
        var opened: Video? = null
        val video = testVideo("a")
        setScreen(flowOf(PagingData.from(listOf(video))), onOpenDetails = { opened = it })
        awaitTag(CatalogTestTags.videoCard("a"))

        composeRule.onNodeWithTag(CatalogTestTags.videoCard("a")).performClick()

        assertEquals(video, opened)
    }

    @Test
    fun back_invokesCallback() {
        var backs = 0
        setScreen(flowOf(PagingData.from(emptyList())), onBack = { backs++ })

        composeRule.onNodeWithTag(BACK_BUTTON_TAG).performClick()

        assertEquals(1, backs)
    }
}
