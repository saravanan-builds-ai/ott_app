package `in`.sms.tringapps.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import `in`.sms.tringapps.catalog.domain.usecase.GetVideoUseCase
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.usecase.ObserveContinueWatchingUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveMyListUseCase
import `in`.sms.tringapps.library.domain.usecase.ToggleMyListUseCase
import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.FakeLibraryRepository
import `in`.sms.tringapps.testing.MainDispatcherRule
import `in`.sms.tringapps.testing.testVideo
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogRepository()
    private val library = FakeLibraryRepository()
    private val video = testVideo("v1")

    private fun TestScope.createViewModel(args: Map<String, Any?> = mapOf(DetailsViewModel.ARG_MEDIA_ID to "v1")): DetailsViewModel {
        val viewModel = DetailsViewModel(
            savedStateHandle = SavedStateHandle(args),
            getVideo = GetVideoUseCase(repository),
            observeMyList = ObserveMyListUseCase(library),
            observeContinueWatching = ObserveContinueWatchingUseCase(library),
            toggleMyListUseCase = ToggleMyListUseCase(library),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `resolves the video from its id`() = runTest {
        repository.videoResults["v1"] = Result.success(video)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(DetailsUiState.Content(video, isInMyList = false, resumeProgress = null), viewModel.uiState.value)
    }

    @Test
    fun `shows resume progress for a started title`() = runTest {
        repository.videoResults["v1"] = Result.success(video)
        library.continueWatching.value = listOf(ContinueWatchingItem(video, 30_000, 120_000, 0))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0.25f, (viewModel.uiState.value as DetailsUiState.Content).resumeProgress)
    }

    @Test
    fun `toggling my list updates the state`() = runTest {
        repository.videoResults["v1"] = Result.success(video)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleMyList()
        advanceUntilIdle()

        assertEquals(true, (viewModel.uiState.value as DetailsUiState.Content).isInMyList)
    }

    @Test
    fun `network failure is retryable`() = runTest {
        repository.videoResults["v1"] = Result.failure(IOException())
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(DetailsUiState.Error(DetailsError.NETWORK), viewModel.uiState.value)

        repository.videoResults["v1"] = Result.success(video)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(video, (viewModel.uiState.value as DetailsUiState.Content).video)
    }

    @Test
    fun `unknown or missing id is not found`() = runTest {
        val unknown = createViewModel()
        val missing = createViewModel(emptyMap())
        advanceUntilIdle()

        assertEquals(DetailsUiState.Error(DetailsError.NOT_FOUND), unknown.uiState.value)
        assertEquals(DetailsUiState.Error(DetailsError.NOT_FOUND), missing.uiState.value)
    }
}
