package `in`.sms.tringapps.feature.home.presentation

import app.cash.turbine.test
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.usecase.GetHomeSectionsUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPagedPlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPlayablePlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetTrendingUseCase
import `in`.sms.tringapps.catalog.domain.usecase.PLAYLIST_PAGE_SIZE
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.usecase.ObserveContinueWatchingUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveMyListUseCase
import `in`.sms.tringapps.library.domain.usecase.ToggleMyListUseCase
import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.FakeLibraryRepository
import `in`.sms.tringapps.testing.MainDispatcherRule
import `in`.sms.tringapps.testing.testPlaylist
import `in`.sms.tringapps.testing.testSection
import `in`.sms.tringapps.testing.testVideo
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogRepository()
    private val library = FakeLibraryRepository()

    private fun createViewModel() = HomeViewModel(
        getHomeSections = GetHomeSectionsUseCase(repository),
        getPlayablePlaylist = GetPlayablePlaylistUseCase(repository),
        getPagedPlaylist = GetPagedPlaylistUseCase(repository),
        getTrending = GetTrendingUseCase(repository),
        observeContinueWatching = ObserveContinueWatchingUseCase(library),
        observeMyList = ObserveMyListUseCase(library),
        toggleMyListUseCase = ToggleMyListUseCase(library),
    )

    private fun content(viewModel: HomeViewModel) = viewModel.uiState.value as HomeUiState.Content

    @Test
    fun `starts loading then shows rails and featured hero`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(testSection("F", featured = true), testSection("A")),
        )
        repository.playlistResults["F"] = Result.success(testPlaylist("F", testVideo("f1"), testVideo("f2")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))

        val viewModel = createViewModel()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals(listOf("F", "A"), state.rails.map { it.playlistId })
        assertTrue(state.rails.none { it.isLoading })
        assertEquals(listOf("f1", "f2"), state.hero.map { it.mediaId })
    }

    @Test
    fun `trending top ten is loaded alongside rails`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        repository.trendingResult = Result.success((1..12).map { testVideo("t$it") })

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(10, content(viewModel).trending.size)
        assertEquals("t1", content(viewModel).trending.first().mediaId)
    }

    @Test
    fun `trending failure hides only the trending rail`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        repository.trendingResult = Result.failure(IOException())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(content(viewModel).trending.isEmpty())
        assertEquals(listOf("A"), content(viewModel).rails.map { it.playlistId })
    }

    @Test
    fun `library rails follow the on-device library`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        library.myList.value = listOf(testVideo("m1"))

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(listOf("m1"), content(viewModel).myList.map { it.mediaId })

        library.continueWatching.value = listOf(ContinueWatchingItem(testVideo("c1"), 30_000, 60_000, 0))
        advanceUntilIdle()

        assertEquals(listOf("c1"), content(viewModel).continueWatching.map { it.video.mediaId })
        assertTrue(content(viewModel).isInMyList("m1"))
    }

    @Test
    fun `toggle my list updates the state`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleMyList(testVideo("a1"))
        advanceUntilIdle()

        assertTrue(content(viewModel).isInMyList("a1"))
    }

    @Test
    fun `rail paging flow is created once per playlist`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val flow = viewModel.railPaging("A")

        assertSame(flow, viewModel.railPaging("A"))
        assertEquals(listOf("A" to PLAYLIST_PAGE_SIZE), repository.pagedRequests)
    }

    @Test
    fun `non playlist sections are not shown`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(testSection("CW", type = SectionType.CONTINUE_WATCHING), testSection("A")),
        )
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("A"), content(viewModel).rails.map { it.playlistId })
    }

    @Test
    fun `rails appear while loading and fill in progressively`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.sectionsResult = Result.success(listOf(testSection("A"), testSection("B", title = "")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        repository.playlistResults["B"] = Result.success(testPlaylist("B", testVideo("b1"), title = "From playlist"))
        repository.playlistGates["B"] = gate

        val viewModel = createViewModel()
        advanceUntilIdle()

        val partial = content(viewModel)
        assertEquals(false, partial.rails.first { it.playlistId == "A" }.isLoading)
        assertEquals(true, partial.rails.first { it.playlistId == "B" }.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()

        val rail = content(viewModel).rails.first { it.playlistId == "B" }
        assertEquals(false, rail.isLoading)
        assertEquals("From playlist", rail.title)
    }

    @Test
    fun `failed and empty rails are dropped`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A"), testSection("BAD"), testSection("EMPTY")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        repository.playlistResults["BAD"] = Result.failure(IOException())
        repository.playlistResults["EMPTY"] = Result.success(testPlaylist("EMPTY", testVideo("x", playable = false)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("A"), content(viewModel).rails.map { it.playlistId })
    }

    @Test
    fun `hero falls back to first loaded rail when featured has nothing playable`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("F", featured = true), testSection("A")))
        repository.playlistResults["F"] = Result.success(testPlaylist("F", testVideo("series", playable = false)))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1"), testVideo("a2")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("a1", "a2"), content(viewModel).hero.map { it.mediaId })
    }

    @Test
    fun `hero is capped`() = runTest {
        val videos = (1..10).map { testVideo("v$it") }.toTypedArray()
        repository.sectionsResult = Result.success(listOf(testSection("F", featured = true)))
        repository.playlistResults["F"] = Result.success(testPlaylist("F", *videos))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeViewModel.HERO_SIZE, content(viewModel).hero.size)
    }

    @Test
    fun `config network failure shows network error`() = runTest {
        repository.sectionsResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(HomeErrorKind.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `config unexpected failure shows generic error`() = runTest {
        repository.sectionsResult = Result.failure(IllegalStateException())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(HomeErrorKind.GENERIC), viewModel.uiState.value)
    }

    @Test
    fun `no sections shows empty state`() = runTest {
        repository.sectionsResult = Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(HomeErrorKind.EMPTY), viewModel.uiState.value)
    }

    @Test
    fun `all playlists failing shows the failure kind`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A"), testSection("B")))
        repository.playlistResults["A"] = Result.failure(IOException())
        repository.playlistResults["B"] = Result.failure(IOException())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(HomeErrorKind.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `all playlists empty shows empty state`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(HomeErrorKind.EMPTY), viewModel.uiState.value)
    }

    @Test
    fun `empty rails with trending still show content`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A"))
        repository.trendingResult = Result.success(listOf(testVideo("t1")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("t1"), content(viewModel).trending.map { it.mediaId })
    }

    @Test
    fun `retry after error reloads content`() = runTest {
        repository.sectionsResult = Result.failure(IOException())
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.sectionsResult = Result.success(listOf(testSection("A")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))

        viewModel.uiState.test {
            assertEquals(HomeUiState.Error(HomeErrorKind.NETWORK), awaitItem())
            viewModel.retry()
            assertEquals(HomeUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        assertEquals(listOf("A"), content(viewModel).rails.map { it.playlistId })
        assertEquals(2, repository.sectionsCalls)
    }
}
