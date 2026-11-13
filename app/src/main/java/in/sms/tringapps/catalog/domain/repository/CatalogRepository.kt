package `in`.sms.tringapps.catalog.domain.repository

import androidx.paging.PagingData
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    suspend fun getSections(): Result<List<Section>>

    /** One page; [offset] is the 1-based index of the first item. */
    suspend fun getPlaylistPage(playlistId: String, offset: Int, limit: Int): Result<PlaylistPage>

    /** Endless paging over a playlist (first page is shared with [getPlaylistPage]'s cache). */
    fun pagedPlaylist(playlistId: String, pageSize: Int): Flow<PagingData<Video>>

    /** First page of the "Trending Now" playlist. */
    suspend fun getTrending(limit: Int): Result<List<Video>>

    suspend fun getVideo(mediaId: String): Result<Video>

    /** Every video loaded so far in this session (used for on-device search). */
    fun cachedVideos(): List<Video>
}

/** Thrown when the backend has no item for the requested id. */
class ContentNotFoundException(id: String) : Exception("No content for id $id")
