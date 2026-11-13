package `in`.sms.tringapps.library.domain.model

import `in`.sms.tringapps.catalog.domain.model.Video

data class ContinueWatchingItem(
    val video: Video,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
) {
    /** 0..1, for the progress bar. */
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}
