package `in`.sms.tringapps.testing

import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLibraryRepository : LibraryRepository {

    val continueWatching = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val myList = MutableStateFlow<List<Video>>(emptyList())

    override fun observeContinueWatching(): Flow<List<ContinueWatchingItem>> = continueWatching
    override fun observeMyList(): Flow<List<Video>> = myList

    override suspend fun getProgress(mediaId: String): ContinueWatchingItem? =
        continueWatching.value.firstOrNull { it.video.mediaId == mediaId }

    override suspend fun saveProgress(video: Video, positionMs: Long, durationMs: Long) {
        continueWatching.value = listOf(ContinueWatchingItem(video, positionMs, durationMs, updatedAtMs = 0)) +
            continueWatching.value.filterNot { it.video.mediaId == video.mediaId }
    }

    override suspend fun removeProgress(mediaId: String) {
        continueWatching.value = continueWatching.value.filterNot { it.video.mediaId == mediaId }
    }

    override suspend fun toggleMyList(video: Video): Boolean {
        val present = myList.value.any { it.mediaId == video.mediaId }
        myList.value = if (present) myList.value.filterNot { it.mediaId == video.mediaId } else listOf(video) + myList.value
        return !present
    }
}
