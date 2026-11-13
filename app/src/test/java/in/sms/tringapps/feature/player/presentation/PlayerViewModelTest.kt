package `in`.sms.tringapps.feature.player.presentation

import androidx.lifecycle.SavedStateHandle
import `in`.sms.tringapps.catalog.domain.usecase.GetVideoUseCase
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.usecase.GetResumePositionUseCase
import `in`.sms.tringapps.library.domain.usecase.SaveWatchProgressUseCase
import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.FakeLibraryRepository
import `in`.sms.tringapps.testing.MainDispatcherRule
import `in`.sms.tringapps.testing.testVideo
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogRepository()
    private val library = FakeLibraryRepository()
    private val savedStateHandle = SavedStateHandle(mapOf(PlayerViewModel.ARG_MEDIA_ID to "v1"))

    private fun TestScope.createViewModel(handle: SavedStateHandle = savedStateHandle) = PlayerViewModel(
        savedStateHandle = handle,
        getVideo = GetVideoUseCase(repository),
        getResumePosition = GetResumePositionUseCase(library),
        saveWatchProgress = SaveWatchProgressUseCase(library),
        // The test scope itself (not backgroundScope) so advanceUntilIdle runs the progress writes.
        appScope = this,
    )

    @Test
    fun `loads a playable video and starts from the beginning`() = runTest {
        val video = testVideo("v1")
        repository.videoResults["v1"] = Result.success(video)

        val viewModel = createViewModel()
        assertEquals(PlayerUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(PlayerUiState.Ready(video, startPositionMs = 0L), viewModel.uiState.value)
    }

    @Test
    fun `resumes from continue watching`() = runTest {
        val video = testVideo("v1")
        repository.videoResults["v1"] = Result.success(video)
        library.continueWatching.value = listOf(ContinueWatchingItem(video, 90_000, 600_000, 0))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(PlayerUiState.Ready(video, startPositionMs = 90_000L), viewModel.uiState.value)
    }

    @Test
    fun `progress is persisted every ten seconds and on close`() = runTest {
        val video = testVideo("v1")
        repository.videoResults["v1"] = Result.success(video)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onProgress(12_000, 600_000)
        advanceUntilIdle()
        assertEquals(12_000L, library.continueWatching.value.single().positionMs)

        viewModel.onProgress(15_000, 600_000)
        advanceUntilIdle()
        assertEquals(12_000L, library.continueWatching.value.single().positionMs)

        viewModel.onProgress(16_000, 600_000, isFinal = true)
        advanceUntilIdle()
        assertEquals(16_000L, library.continueWatching.value.single().positionMs)
    }

    @Test
    fun `unknown player duration falls back to catalog duration`() = runTest {
        val video = testVideo("v1", durationSeconds = 600)
        repository.videoResults["v1"] = Result.success(video)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onProgress(590_000, durationMs = 0, isFinal = true)
        advanceUntilIdle()

        // 590s of 600s is past the finished threshold, so it leaves Continue Watching.
        assertTrue(library.continueWatching.value.isEmpty())
    }

    @Test
    fun `video without stream is not playable`() = runTest {
        repository.videoResults["v1"] = Result.success(testVideo("v1", playable = false))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(PlayerUiState.Error(PlayerError.NOT_PLAYABLE), viewModel.uiState.value)
    }

    @Test
    fun `network failure is retryable`() = runTest {
        repository.videoResults["v1"] = Result.failure(IOException())
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(PlayerUiState.Error(PlayerError.NETWORK), viewModel.uiState.value)

        repository.videoResults["v1"] = Result.success(testVideo("v1"))
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PlayerUiState.Ready)
    }

    @Test
    fun `unknown media is not found`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(PlayerUiState.Error(PlayerError.NOT_FOUND), viewModel.uiState.value)
    }

    @Test
    fun `missing argument is not found`() = runTest {
        val viewModel = createViewModel(SavedStateHandle())
        advanceUntilIdle()

        assertEquals(PlayerUiState.Error(PlayerError.NOT_FOUND), viewModel.uiState.value)
    }

    @Test
    fun `playback error then retry resumes from the saved position`() = runTest {
        val video = testVideo("v1")
        repository.videoResults["v1"] = Result.success(video)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onProgress(42_000L, 600_000L)
        viewModel.onPlaybackError()
        assertEquals(PlayerUiState.Error(PlayerError.PLAYBACK), viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(PlayerUiState.Ready(video, startPositionMs = 42_000L), viewModel.uiState.value)
    }

    @Test
    fun `saved position survives view model recreation`() = runTest {
        val video = testVideo("v1")
        repository.videoResults["v1"] = Result.success(video)
        val first = createViewModel()
        advanceUntilIdle()
        first.onProgress(7_000L, 600_000L)

        val restored = createViewModel()
        advanceUntilIdle()

        assertEquals(PlayerUiState.Ready(video, startPositionMs = 7_000L), restored.uiState.value)
    }
}
