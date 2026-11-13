package `in`.sms.tringapps.feature.seeall.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import `in`.sms.tringapps.catalog.domain.usecase.GetPagedPlaylistUseCase
import `in`.sms.tringapps.catalog.domain.usecase.PLAYLIST_PAGE_SIZE
import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.MainDispatcherRule
import `in`.sms.tringapps.testing.testPlaylist
import `in`.sms.tringapps.testing.testVideo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SeeAllViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogRepository()

    private fun createViewModel(args: Map<String, Any?>) =
        SeeAllViewModel(SavedStateHandle(args), GetPagedPlaylistUseCase(repository))

    @Test
    fun `exposes the title and pages the requested playlist`() = runTest {
        repository.playlistResults["P"] = Result.success(testPlaylist("P", testVideo("1")))

        val viewModel = createViewModel(
            mapOf(SeeAllViewModel.ARG_PLAYLIST_ID to "P", SeeAllViewModel.ARG_TITLE to "Novedades"),
        )

        assertEquals("Novedades", viewModel.title)
        assertEquals(listOf("P" to PLAYLIST_PAGE_SIZE), repository.pagedRequests)
    }

    @Test
    fun `missing playlist id yields an empty grid`() = runTest {
        val viewModel = createViewModel(emptyMap())

        assertEquals("", viewModel.title)
        assertTrue(viewModel.videos.asSnapshot().isEmpty())
        assertTrue(repository.pagedRequests.isEmpty())
    }
}
