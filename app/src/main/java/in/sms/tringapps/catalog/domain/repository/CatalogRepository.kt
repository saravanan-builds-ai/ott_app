package `in`.sms.tringapps.catalog.domain.repository

import androidx.paging.PagingData
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    suspend fun getSections(): Result<List<Section>>

    /** One page; [offset] is the 1-based index of the first item. Pages are cached per session. */
    suspend fun getPlaylistPage(playlistId: String, offset: Int, limit: Int): Result<PlaylistPage>

    /** Endless paging over a playlist (pages are shared with [getPlaylistPage]'s cache). */
    fun pagedPlaylist(playlistId: String, pageSize: Int): Flow<PagingData<Video>>

    suspend fun getVideo(mediaId: String): Result<Video>

    /** Every video loaded so far in this session (used for on-device search). */
    fun cachedVideos(): List<Video>

    /** Drops cached pages and videos so the next requests hit the network (pull-to-refresh). */
    fun clearCache()
}

/** Thrown when the backend has no item for the requested id. */
class ContentNotFoundException(id: String) : Exception("No content for id $id")
