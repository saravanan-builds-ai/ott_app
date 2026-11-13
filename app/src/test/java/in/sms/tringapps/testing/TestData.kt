package `in`.sms.tringapps.testing

import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.model.Stream
import `in`.sms.tringapps.catalog.domain.model.Video

fun testVideo(
    id: String,
    title: String = "Title $id",
    playable: Boolean = true,
    durationSeconds: Long = 600,
) = Video(
    mediaId = id,
    title = title,
    description = "Description $id",
    thumbnailUrl = null,
    backdropUrl = null,
    durationSeconds = durationSeconds,
    ageRating = "16+",
    stream = if (playable) Stream("https://cdn.jwplayer.com/manifests/$id.m3u8", isHls = true) else null,
)

fun testSection(
    id: String,
    title: String = "Section $id",
    type: SectionType = SectionType.PLAYLIST,
    featured: Boolean = false,
) = Section(playlistId = id, title = title, type = type, featured = featured)

fun testPlaylist(id: String, vararg videos: Video, title: String = "Playlist $id") =
    Playlist(id = id, title = title, videos = videos.toList())
