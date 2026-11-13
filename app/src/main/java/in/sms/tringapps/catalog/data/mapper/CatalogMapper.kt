package `in`.sms.tringapps.catalog.data.mapper

import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.model.Stream
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.network.dto.AppConfigDto
import `in`.sms.tringapps.core.network.dto.ImageDto
import `in`.sms.tringapps.core.network.dto.MediaItemDto
import `in`.sms.tringapps.core.network.dto.PlaylistResponseDto
import `in`.sms.tringapps.core.network.dto.SourceDto

internal const val HLS_MIME_TYPE = "application/vnd.apple.mpegurl"
private val HLS_MIME_TYPES = setOf(HLS_MIME_TYPE, "application/x-mpegurl")
private const val MP4_MIME_TYPE = "video/mp4"
private const val THUMBNAIL_WIDTH = 640
private const val BACKDROP_WIDTH = 1280

fun AppConfigDto.toSections(): List<Section> = content.mapNotNull { dto ->
    val id = dto.contentId?.trim().orEmpty()
    if (id.isEmpty()) return@mapNotNull null
    Section(
        playlistId = id,
        title = dto.title?.trim().orEmpty(),
        type = dto.type.toSectionType(),
        featured = dto.featured,
    )
}

internal fun String?.toSectionType(): SectionType = when (this?.lowercase()) {
    "playlist" -> SectionType.PLAYLIST
    "continue_watching" -> SectionType.CONTINUE_WATCHING
    "favorites" -> SectionType.FAVORITES
    else -> SectionType.UNKNOWN
}

fun PlaylistResponseDto.toPlaylist(requestedId: String): Playlist = Playlist(
    id = feedid ?: requestedId,
    title = title?.trim().orEmpty(),
    videos = playlist.mapNotNull { it.toVideoOrNull() },
)

/**
 * Page mapping. The next offset is read from JW's `links.next` (`page_offset` query param);
 * it must move forward, otherwise paging stops (guards against a misbehaving backend).
 */
fun PlaylistResponseDto.toPlaylistPage(requestedId: String, currentOffset: Int): PlaylistPage =
    PlaylistPage(
        playlist = toPlaylist(requestedId),
        nextOffset = links?.next?.pageOffsetOrNull()?.takeIf { it > currentOffset && playlist.isNotEmpty() },
    )

private val PAGE_OFFSET = "[?&]page_offset=(\\d+)".toRegex()

internal fun String.pageOffsetOrNull(): Int? =
    PAGE_OFFSET.find(this)?.groupValues?.get(1)?.toIntOrNull()

fun MediaItemDto.toVideoOrNull(): Video? {
    val id = mediaid?.trim().orEmpty()
    if (id.isEmpty()) return null
    return Video(
        mediaId = id,
        title = title?.trim().orEmpty(),
        description = description.cleanDescription(),
        thumbnailUrl = images.pickImage(THUMBNAIL_WIDTH) ?: image.validUrlOrNull(),
        backdropUrl = images.pickImage(BACKDROP_WIDTH) ?: image.validUrlOrNull(),
        durationSeconds = duration?.takeIf { it > 0 }?.toLong() ?: 0L,
        ageRating = agerating?.trim()?.takeIf { it.isNotEmpty() },
        stream = sources.selectStream(),
    )
}

/**
 * Strips whitespace and treats placeholder-only strings (e.g. `"..."`, `"…"`, `"-"` or `"N/A"`)
 * as empty so UI components don't render meaningless text when a description is missing.
 */
internal fun String?.cleanDescription(): String {
    val trimmed = this?.trim().orEmpty()
    if (trimmed.isEmpty()) return ""
    if (trimmed.equals("n/a", ignoreCase = true) || trimmed.equals("none", ignoreCase = true)) return ""
    return if (trimmed.any { it.isLetterOrDigit() }) trimmed else ""
}

/**
 * Picks the HLS (`.m3u8`) source when present; otherwise falls back to the
 * highest-resolution progressive MP4. Placeholder files (e.g. `"None"`) are ignored.
 */
internal fun List<SourceDto>.selectStream(): Stream? {
    val valid = filter { it.file.validUrlOrNull() != null }
    valid.firstOrNull { it.isHls() }?.let { return Stream(url = it.file!!, isHls = true) }
    return valid
        .filter { it.type.equals(MP4_MIME_TYPE, ignoreCase = true) }
        .maxByOrNull { it.height ?: 0 }
        ?.let { Stream(url = it.file!!, isHls = false) }
}

private fun SourceDto.isHls(): Boolean =
    type?.lowercase() in HLS_MIME_TYPES ||
        file.orEmpty().substringBefore('?').endsWith(".m3u8", ignoreCase = true)

/** The smallest image at least [minWidth] wide, else the largest available. */
private fun List<ImageDto>.pickImage(minWidth: Int): String? {
    val valid = filter { it.src.validUrlOrNull() != null }
    return (valid.filter { (it.width ?: 0) >= minWidth }.minByOrNull { it.width ?: 0 }
        ?: valid.maxByOrNull { it.width ?: 0 })?.src
}

private fun String?.validUrlOrNull(): String? =
    this?.trim()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
