package `in`.sms.tringapps.feature.search.presentation

import `in`.sms.tringapps.catalog.domain.usecase.SearchCatalogUseCase
import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.MainDispatcherRule
import `in`.sms.tringapps.testing.testVideo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogRepository().apply {
        cached = listOf(testVideo("1", title = "Un perro andaluz"), testVideo("2", title = "Almas pasajeras"))
    }

    private fun TestScope.createViewModel(): SearchViewModel {
        val viewModel = SearchViewModel(SearchCatalogUseCase(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `starts with suggestions`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SearchStatus.SUGGESTIONS, viewModel.uiState.value.status)
        assertEquals(listOf("1", "2"), viewModel.uiState.value.videos.map { it.mediaId })
    }

    @Test
    fun `nothing loaded yet is reported`() = runTest {
        repository.cached = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SearchStatus.NOTHING_LOADED, viewModel.uiState.value.status)
    }

    @Test
    fun `query is debounced before searching`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("perro")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS - 50)
        assertEquals(SearchStatus.SUGGESTIONS, viewModel.uiState.value.status)

        advanceTimeBy(100)
        val state = viewModel.uiState.value
        assertEquals(SearchStatus.RESULTS, state.status)
        assertEquals("perro", state.query)
        assertEquals(listOf("1"), state.videos.map { it.mediaId })
    }

    @Test
    fun `typing quickly searches only the last query`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("p")
        advanceTimeBy(100)
        viewModel.onQueryChange("alm")
        advanceUntilIdle()

        assertEquals("alm", viewModel.uiState.value.query)
        assertEquals(listOf("2"), viewModel.uiState.value.videos.map { it.mediaId })
    }

    @Test
    fun `no match reports no results`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("zzz")
        advanceUntilIdle()

        assertEquals(SearchStatus.NO_RESULTS, viewModel.uiState.value.status)
    }

    @Test
    fun `clearing the query returns to suggestions immediately`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("perro")
        advanceUntilIdle()

        viewModel.onQueryChange("")
        advanceTimeBy(1)

        assertEquals(SearchStatus.SUGGESTIONS, viewModel.uiState.value.status)
    }
}
