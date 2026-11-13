package `in`.sms.tringapps.catalog.domain.model

/** A content shelf declared in the JW app config. */
data class Section(
    val playlistId: String,
    val title: String,
    val type: SectionType,
    val featured: Boolean,
)

enum class SectionType { PLAYLIST, CONTINUE_WATCHING, FAVORITES, UNKNOWN }

data class Playlist(
    val id: String,
    val title: String,
    val videos: List<Video>,
)

/** One page of a playlist; [nextOffset] is the 1-based offset of the next page, or null at the end. */
data class PlaylistPage(
    val playlist: Playlist,
    val nextOffset: Int?,
)

data class Video(
    val mediaId: String,
    val title: String,
    val description: String,
    /** Card-sized poster (~640px). */
    val thumbnailUrl: String?,
    /** Large poster for hero/backdrop (~1280px). */
    val backdropUrl: String?,
    val durationSeconds: Long,
    val ageRating: String?,
    val stream: Stream?,
) {
    val isPlayable: Boolean get() = stream != null
}

/** Playback source. [isHls] is true for `.m3u8` manifests. */
data class Stream(
    val url: String,
    val isHls: Boolean,
)
