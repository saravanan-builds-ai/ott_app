package `in`.sms.tringapps.core.media

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import `in`.sms.tringapps.catalog.domain.model.Video

/** Media3 item for a playable [Video]; HLS gets an explicit MIME type so ExoPlayer picks HlsMediaSource. */
fun Video.toMediaItem(): MediaItem? {
    val stream = stream ?: return null
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(stream.url)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .apply { if (stream.isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .build()
}
