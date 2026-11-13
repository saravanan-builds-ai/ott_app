package `in`.sms.tringapps.library.data

import `in`.sms.tringapps.catalog.domain.model.Stream
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import kotlinx.serialization.Serializable

/** Snapshot of a video persisted on device, so library rails render without network. */
@Serializable
data class StoredVideo(
    val mediaId: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String? = null,
    val backdropUrl: String? = null,
    val durationSeconds: Long = 0,
    val ageRating: String? = null,
    val streamUrl: String? = null,
    val isHls: Boolean = false,
)

@Serializable
data class StoredProgress(
    val video: StoredVideo,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
)

@Serializable
data class StoredMyListEntry(
    val video: StoredVideo,
    val addedAtMs: Long,
)

fun Video.toStored() = StoredVideo(
    mediaId = mediaId,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    backdropUrl = backdropUrl,
    durationSeconds = durationSeconds,
    ageRating = ageRating,
    streamUrl = stream?.url,
    isHls = stream?.isHls ?: false,
)

fun StoredVideo.toVideo() = Video(
    mediaId = mediaId,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    backdropUrl = backdropUrl,
    durationSeconds = durationSeconds,
    ageRating = ageRating,
    stream = streamUrl?.let { Stream(it, isHls) },
)

fun StoredProgress.toItem() = ContinueWatchingItem(
    video = video.toVideo(),
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAtMs = updatedAtMs,
)
