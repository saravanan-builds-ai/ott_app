package `in`.sms.tringapps.catalog.domain.usecase

import androidx.paging.testing.asSnapshot
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.testPlaylist
import `in`.sms.tringapps.testing.testSection
import `in`.sms.tringapps.testing.testVideo
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogUseCasesTest {

    private val repository = FakeCatalogRepository()

    @Test
    fun `home content keeps playlists in config order without duplicates`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(
                testSection("A"),
                testSection("B", featured = true),
                testSection("A", title = "Duplicate"),
                testSection("U", type = SectionType.UNKNOWN),
            ),
        )

        val layout = GetHomeContentUseCase(repository)().getOrThrow()

        assertEquals(listOf("A", "B"), layout.playlists.map { it.playlistId })
        assertEquals("Section A", layout.playlists.first().title)
        assertEquals("B", layout.featuredPlaylistId)
    }

    @Test
    fun `special sections are separated and keep their config titles`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(
                testSection("A"),
                testSection("CW", title = "Continuar viendo", type = SectionType.CONTINUE_WATCHING),
                testSection("FAV", title = "Favoritos", type = SectionType.FAVORITES),
            ),
        )

        val layout = GetHomeContentUseCase(repository)().getOrThrow()

        assertEquals(listOf("A"), layout.playlists.map { it.playlistId })
        assertEquals("Continuar viendo", layout.continueWatching?.title)
        assertEquals("Favoritos", layout.favorites?.title)
        assertNull(layout.featuredPlaylistId)
    }

    @Test
    fun `missing special sections are null`() = runTest {
        repository.sectionsResult = Result.success(listOf(testSection("A")))

        val layout = GetHomeContentUseCase(repository)().getOrThrow()

        assertNull(layout.continueWatching)
        assertNull(layout.favorites)
    }

    @Test
    fun `force refresh clears the cache first`() = runTest {
        GetHomeContentUseCase(repository)()
        assertEquals(0, repository.clearCacheCalls)

        GetHomeContentUseCase(repository)(forceRefresh = true)
        assertEquals(1, repository.clearCacheCalls)
    }

    @Test
    fun `home content propagates failure`() = runTest {
        repository.sectionsResult = Result.failure(IOException("offline"))
        assertTrue(GetHomeContentUseCase(repository)().exceptionOrNull() is IOException)
    }

    @Test
    fun `playable playlist drops items without a stream`() = runTest {
        repository.playlistResults["P"] = Result.success(
            testPlaylist("P", testVideo("1"), testVideo("2", playable = false), testVideo("3")),
        )

        val playlist = GetPlayablePlaylistUseCase(repository)("P").getOrThrow()

        assertEquals(listOf("1", "3"), playlist.videos.map { it.mediaId })
    }

    @Test
    fun `paged playlist uses a page size of 25 and filters unplayable items`() = runTest {
        repository.playlistResults["P"] = Result.success(
            testPlaylist("P", testVideo("1"), testVideo("2", playable = false), testVideo("3")),
        )

        val items = GetPagedPlaylistUseCase(repository)("P").asSnapshot()

        assertEquals(listOf("1", "3"), items.map { it.mediaId })
        assertEquals(listOf("P" to 25), repository.pagedRequests)
    }

    @Test
    fun `get video delegates to repository`() = runTest {
        repository.videoResults["v"] = Result.success(testVideo("v"))
        assertEquals("v", GetVideoUseCase(repository)("v").getOrThrow().mediaId)
        assertTrue(GetVideoUseCase(repository)("missing").isFailure)
    }
}
