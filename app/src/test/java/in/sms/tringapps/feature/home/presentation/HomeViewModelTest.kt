package `in`.sms.tringapps.feature.home.presentation

import app.cash.turbine.test
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.usecase.GetHomeContentUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPagedPlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.GetPlayablePlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.PLAYLIST_PAGE_SIZE
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.usecase.ObserveContinueWatchingUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveMyListUseCase
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
import org.junit.Assert.assertNull
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
        getHomeContent = GetHomeContentUseCase(repository),
        getPlayablePlaylist = GetPlayablePlaylistUseCase(repository),
        getPagedPlaylist = GetPagedPlaylistUseCase(repository),
        observeContinueWatching = ObserveContinueWatchingUseCase(library),
        observeMyList = ObserveMyListUseCase(library),
    )

    private fun content(viewModel: HomeViewModel) = viewModel.uiState.value as HomeUiState.Content

    private fun givenTwoPlaylists() {
        repository.sectionsResult = Result.success(listOf(testSection("F", featured = true), testSection("A")))
        repository.playlistResults["F"] = Result.success(testPlaylist("F", testVideo("f1"), testVideo("f2")))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
    }

    @Test
    fun `starts loading then shows rails with the featured playlist as hero and top ten`() = runTest {
        givenTwoPlaylists()

        val viewModel = createViewModel()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals("F", state.featured?.playlistId)
        assertEquals(listOf("f1", "f2"), state.hero.map { it.mediaId })
        assertEquals(listOf("f1", "f2"), state.topTen.map { it.mediaId })
        // The featured playlist is shown as Top 10, not repeated as a plain rail.
        assertEquals(listOf("A"), state.visibleRails.map { it.playlistId })
    }

    @Test
    fun `hero and top ten are capped`() = runTest {
        val videos = (1..15).map { testVideo("v$it") }.toTypedArray()
        repository.sectionsResult = Result.success(listOf(testSection("F", featured = true)))
        repository.playlistResults["F"] = Result.success(testPlaylist("F", *videos))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HERO_SIZE, content(viewModel).hero.size)
        assertEquals(TOP_TEN_SIZE, content(viewModel).topTen.size)
    }

    @Test
    fun `featured falls back to the first loaded playlist when featured has nothing playable`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.sectionsResult = Result.success(
            listOf(testSection("F", featured = true), testSection("A"), testSection("B")),
        )
        repository.playlistResults["F"] = Result.success(testPlaylist("F", testVideo("series", playable = false)))
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        repository.playlistResults["B"] = Result.success(testPlaylist("B", testVideo("b1")))
        repository.playlistGates["A"] = gate

        val viewModel = createViewModel()
        advanceUntilIdle()
        // B loaded first while A is still in flight, so B immediately drives the hero.
        assertEquals("B", content(viewModel).featured?.playlistId)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("B", content(viewModel).featured?.playlistId)
    }

    @Test
    fun `special sections use their config titles and follow the library`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(
                testSection("A"),
                testSection("CW", title = "Continuar viendo", type = SectionType.CONTINUE_WATCHING),
                testSection("FAV", title = "Favoritos", type = SectionType.FAVORITES),
            ),
        )
        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1")))
        library.myList.value = listOf(testVideo("m1"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Continuar viendo", content(viewModel).continueWatchingTitle)
        assertEquals("Favoritos", content(viewModel).myListTitle)
        assertEquals(listOf("m1"), content(viewModel).myList.map { it.mediaId })

        library.continueWatching.value = listOf(ContinueWatchingItem(testVideo("c1"), 30_000, 60_000, 0))
        advanceUntilIdle()

        assertEquals(listOf("c1"), content(viewModel).continueWatching.map { it.video.mediaId })
    }

    @Test
    fun `special sections are absent when the config does not declare them`() = runTest {
        givenTwoPlaylists()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(content(viewModel).continueWatchingTitle)
        assertNull(content(viewModel).myListTitle)
    }

    @Test
    fun `rail paging flow is created once per playlist with the shared page size`() = runTest {
        givenTwoPlaylists()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val flow = viewModel.railPaging("A")

        assertSame(flow, viewModel.railPaging("A"))
        assertEquals(listOf("A" to PLAYLIST_PAGE_SIZE), repository.pagedRequests)
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
    fun `config without playlists shows empty state`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(testSection("CW", type = SectionType.CONTINUE_WATCHING)),
        )

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
    fun `retry after error reloads content`() = runTest {
        repository.sectionsResult = Result.failure(IOException())
        val viewModel = createViewModel()
        advanceUntilIdle()

        givenTwoPlaylists()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Error(HomeErrorKind.NETWORK), awaitItem())
            viewModel.retry()
            assertEquals(HomeUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        assertEquals(listOf("A"), content(viewModel).visibleRails.map { it.playlistId })
        assertEquals(2, repository.sectionsCalls)
    }

    @Test
    fun `refresh keeps content visible, clears the cache and swaps in new data`() = runTest {
        givenTwoPlaylists()
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.playlistResults["A"] = Result.success(testPlaylist("A", testVideo("a1"), testVideo("a2")))
        viewModel.uiState.test {
            assertEquals(1, (awaitItem() as HomeUiState.Content).rails.first { it.playlistId == "A" }.videos.size)
            viewModel.refresh()
            val refreshing = awaitItem() as HomeUiState.Content
            assertTrue(refreshing.isRefreshing)
            assertEquals(1, refreshing.rails.first { it.playlistId == "A" }.videos.size)
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        val refreshed = content(viewModel)
        assertEquals(false, refreshed.isRefreshing)
        assertEquals(2, refreshed.rails.first { it.playlistId == "A" }.videos.size)
        assertEquals(1, repository.clearCacheCalls)
    }

    @Test
    fun `failed refresh keeps the previous content and reports the error once`() = runTest {
        givenTwoPlaylists()
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.sectionsResult = Result.failure(IOException())
        viewModel.refresh()
        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals(HomeErrorKind.NETWORK, state.refreshError)
        assertEquals(false, state.isRefreshing)
        assertEquals(listOf("A"), state.visibleRails.map { it.playlistId })

        viewModel.onRefreshErrorShown()
        assertNull(content(viewModel).refreshError)
    }
}
