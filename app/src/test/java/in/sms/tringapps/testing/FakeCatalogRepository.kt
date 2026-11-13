package `in`.sms.tringapps.testing

import androidx.paging.PagingData
import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.repository.CatalogRepository
import `in`.sms.tringapps.catalog.domain.repository.ContentNotFoundException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeCatalogRepository : CatalogRepository {

    var sectionsResult: Result<List<Section>> = Result.success(emptyList())

    /** First page of each playlist. */
    val playlistResults = mutableMapOf<String, Result<Playlist>>()
    val videoResults = mutableMapOf<String, Result<Video>>()
    var cached: List<Video> = emptyList()

    /** When set for an id, [getPlaylistPage] suspends until the deferred completes. */
    val playlistGates = mutableMapOf<String, CompletableDeferred<Unit>>()

    var sectionsCalls = 0
        private set
    val pagedRequests = mutableListOf<Pair<String, Int>>()

    override suspend fun getSections(): Result<List<Section>> {
        sectionsCalls++
        return sectionsResult
    }

    override suspend fun getPlaylistPage(playlistId: String, offset: Int, limit: Int): Result<PlaylistPage> {
        playlistGates[playlistId]?.await()
        val result = playlistResults[playlistId] ?: Result.failure(ContentNotFoundException(playlistId))
        return result.map { PlaylistPage(it, nextOffset = null) }
    }

    override fun pagedPlaylist(playlistId: String, pageSize: Int): Flow<PagingData<Video>> {
        pagedRequests += playlistId to pageSize
        val videos = playlistResults[playlistId]?.getOrNull()?.videos.orEmpty()
        return flowOf(PagingData.from(videos))
    }

    override suspend fun getVideo(mediaId: String): Result<Video> =
        videoResults[mediaId] ?: Result.failure(ContentNotFoundException(mediaId))

    override fun cachedVideos(): List<Video> = cached

    var clearCacheCalls = 0
        private set

    override fun clearCache() {
        clearCacheCalls++
    }
}
