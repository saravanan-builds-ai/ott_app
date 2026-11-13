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
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogUseCasesTest {

    private val repository = FakeCatalogRepository()

    @Test
    fun `home sections keep only playlists without duplicates in order`() = runTest {
        repository.sectionsResult = Result.success(
            listOf(
                testSection("A"),
                testSection("CW", type = SectionType.CONTINUE_WATCHING),
                testSection("FAV", type = SectionType.FAVORITES),
                testSection("B"),
                testSection("A", title = "Duplicate"),
                testSection("U", type = SectionType.UNKNOWN),
            ),
        )

        val sections = GetHomeSectionsUseCase(repository)().getOrThrow()

        assertEquals(listOf("A", "B"), sections.map { it.playlistId })
        assertEquals("Section A", sections.first().title)
    }

    @Test
    fun `home sections propagate failure`() = runTest {
        repository.sectionsResult = Result.failure(IOException("offline"))
        assertTrue(GetHomeSectionsUseCase(repository)().exceptionOrNull() is IOException)
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
    fun `paged playlist uses the shared page size and filters unplayable items`() = runTest {
        repository.playlistResults["P"] = Result.success(
            testPlaylist("P", testVideo("1"), testVideo("2", playable = false), testVideo("3")),
        )

        val items = GetPagedPlaylistUseCase(repository)("P").asSnapshot()

        assertEquals(listOf("1", "3"), items.map { it.mediaId })
        assertEquals(listOf("P" to PLAYLIST_PAGE_SIZE), repository.pagedRequests)
    }

    @Test
    fun `trending keeps playable unique items capped at ten`() = runTest {
        val videos = (1..15).map { testVideo("t$it", playable = it != 2) } + testVideo("t1")
        repository.trendingResult = Result.success(videos)

        val trending = GetTrendingUseCase(repository)().getOrThrow()

        assertEquals(TRENDING_SIZE, trending.size)
        assertEquals(listOf("t1", "t3", "t4"), trending.take(3).map { it.mediaId })
        assertEquals(trending.size, trending.distinctBy { it.mediaId }.size)
    }

    @Test
    fun `get video delegates to repository`() = runTest {
        repository.videoResults["v"] = Result.success(testVideo("v"))
        assertEquals("v", GetVideoUseCase(repository)("v").getOrThrow().mediaId)
        assertTrue(GetVideoUseCase(repository)("missing").isFailure)
    }
}
