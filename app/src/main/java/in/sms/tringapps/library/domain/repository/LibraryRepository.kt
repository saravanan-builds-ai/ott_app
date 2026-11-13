package `in`.sms.tringapps.library.domain.repository

import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import kotlinx.coroutines.flow.Flow

/** On-device user library: watch progress and "My List". */
interface LibraryRepository {
    /** Most recently watched first. */
    fun observeContinueWatching(): Flow<List<ContinueWatchingItem>>

    /** Most recently added first. */
    fun observeMyList(): Flow<List<Video>>

    suspend fun getProgress(mediaId: String): ContinueWatchingItem?
    suspend fun saveProgress(video: Video, positionMs: Long, durationMs: Long)
    suspend fun removeProgress(mediaId: String)

    /** Adds when absent, removes when present. Returns true when the video is now in the list. */
    suspend fun toggleMyList(video: Video): Boolean
}
