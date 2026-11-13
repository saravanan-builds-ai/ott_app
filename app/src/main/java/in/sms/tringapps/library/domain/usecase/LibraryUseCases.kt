package `in`.sms.tringapps.library.domain.usecase

import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.repository.LibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Below this we don't consider the title "started". */
const val MIN_RESUME_POSITION_MS = 10_000L

/** At or above this fraction the title counts as finished and leaves Continue Watching. */
const val FINISHED_FRACTION = 0.95

class ObserveContinueWatchingUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(): Flow<List<ContinueWatchingItem>> = repository.observeContinueWatching()
}

class ObserveMyListUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke(): Flow<List<Video>> = repository.observeMyList()
}

class ToggleMyListUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(video: Video): Boolean = repository.toggleMyList(video)
}

/**
 * Records progress: ignored before [MIN_RESUME_POSITION_MS], removed once finished,
 * stored otherwise.
 */
class SaveWatchProgressUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(video: Video, positionMs: Long, durationMs: Long) {
        when {
            durationMs > 0 && positionMs >= durationMs * FINISHED_FRACTION ->
                repository.removeProgress(video.mediaId)
            positionMs < MIN_RESUME_POSITION_MS -> Unit
            else -> repository.saveProgress(video, positionMs, durationMs)
        }
    }
}

/** Where to resume a title, or 0 to start from the beginning. */
class GetResumePositionUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(mediaId: String): Long {
        val item = repository.getProgress(mediaId) ?: return 0L
        val finished = item.durationMs > 0 && item.positionMs >= item.durationMs * FINISHED_FRACTION
        return if (finished || item.positionMs < MIN_RESUME_POSITION_MS) 0L else item.positionMs
    }
}
