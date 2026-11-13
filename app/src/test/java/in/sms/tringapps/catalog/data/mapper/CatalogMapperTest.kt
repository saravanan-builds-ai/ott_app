package `in`.sms.tringapps.catalog.data.mapper

import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.model.Stream
import `in`.sms.tringapps.core.network.dto.AppConfigDto
import `in`.sms.tringapps.core.network.dto.ContentDto
import `in`.sms.tringapps.core.network.dto.ImageDto
import `in`.sms.tringapps.core.network.dto.LinksDto
import `in`.sms.tringapps.core.network.dto.MediaItemDto
import `in`.sms.tringapps.core.network.dto.PlaylistResponseDto
import `in`.sms.tringapps.core.network.dto.SourceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMapperTest {

    private val hls = SourceDto("https://cdn.jwplayer.com/manifests/abc.m3u8", HLS_MIME_TYPE)
    private val mp4Low = SourceDto("https://cdn.jwplayer.com/videos/abc-180.mp4", "video/mp4", 180)
    private val mp4High = SourceDto("https://cdn.jwplayer.com/videos/abc-720.mp4", "video/mp4", 720)

    @Test
    fun `config maps sections in order and skips entries without id`() {
        val dto = AppConfigDto(
            content = listOf(
                ContentDto(contentId = "A1", title = " Featured ", type = "playlist", featured = true),
                ContentDto(contentId = " ", title = "Broken", type = "playlist"),
                ContentDto(contentId = "C1", title = "Continue", type = "continue_watching"),
                ContentDto(contentId = "F1", title = "Favs", type = "favorites"),
                ContentDto(contentId = "X1", title = "Odd", type = null),
            ),
        )

        val sections = dto.toSections()

        assertEquals(listOf("A1", "C1", "F1", "X1"), sections.map { it.playlistId })
        assertEquals("Featured", sections[0].title)
        assertTrue(sections[0].featured)
        assertEquals(
            listOf(SectionType.PLAYLIST, SectionType.CONTINUE_WATCHING, SectionType.FAVORITES, SectionType.UNKNOWN),
            sections.map { it.type },
        )
    }

    @Test
    fun `HLS source is preferred even when listed after mp4`() {
        assertEquals(Stream(hls.file!!, isHls = true), listOf(mp4High, hls).selectStream())
    }

    @Test
    fun `m3u8 extension is detected as HLS when mime type is missing`() {
        val source = SourceDto("https://cdn.example.com/master.m3u8?token=1", type = null)
        assertEquals(true, listOf(source).selectStream()?.isHls)
    }

    @Test
    fun `falls back to highest resolution mp4 without HLS`() {
        assertEquals(Stream(mp4High.file!!, isHls = false), listOf(mp4Low, mp4High).selectStream())
    }

    @Test
    fun `placeholder or empty sources are not playable`() {
        assertNull(listOf(SourceDto("None", "application/octet-stream")).selectStream())
        assertNull(emptyList<SourceDto>().selectStream())
        assertNull(listOf(SourceDto(null, HLS_MIME_TYPE)).selectStream())
    }

    @Test
    fun `media item maps fields and picks image sizes`() {
        val dto = MediaItemDto(
            mediaid = "abc",
            title = " Almas pasajeras ",
            description = "A drama",
            image = "https://cdn.jwplayer.com/v2/media/abc/poster.jpg?width=720",
            images = listOf(
                ImageDto("https://img/320", 320),
                ImageDto("https://img/1920", 1920),
                ImageDto("https://img/640", 640),
                ImageDto("https://img/1280", 1280),
            ),
            duration = 5566.0,
            sources = listOf(hls),
            agerating = "16+",
        )

        val video = dto.toVideoOrNull()!!

        assertEquals("abc", video.mediaId)
        assertEquals("Almas pasajeras", video.title)
        assertEquals("https://img/640", video.thumbnailUrl)
        assertEquals("https://img/1280", video.backdropUrl)
        assertEquals(5566L, video.durationSeconds)
        assertEquals("16+", video.ageRating)
        assertTrue(video.isPlayable)
    }

    @Test
    fun `media item falls back to largest image then poster url`() {
        val small = MediaItemDto(mediaid = "a", images = listOf(ImageDto("https://img/320", 320)))
        assertEquals("https://img/320", small.toVideoOrNull()?.backdropUrl)

        val posterOnly = MediaItemDto(mediaid = "b", image = "https://img/poster.jpg")
        assertEquals("https://img/poster.jpg", posterOnly.toVideoOrNull()?.thumbnailUrl)
    }

    @Test
    fun `media item without id is dropped and unknown duration is zero`() {
        assertNull(MediaItemDto(mediaid = "").toVideoOrNull())
        val series = MediaItemDto(mediaid = "s", duration = 0.0, agerating = "", sources = emptyList())
        val video = series.toVideoOrNull()!!
        assertEquals(0L, video.durationSeconds)
        assertNull(video.ageRating)
        assertFalse(video.isPlayable)
    }

    @Test
    fun `page offset is read from the next link`() {
        assertEquals(21, "https://cdn.jwplayer.com/v2/playlists/X?page_limit=10&page_offset=21&format=json".pageOffsetOrNull())
        assertEquals(11, "https://cdn.jwplayer.com/v2/playlists/X?page_offset=11".pageOffsetOrNull())
        assertNull("https://cdn.jwplayer.com/v2/playlists/X?page_limit=10".pageOffsetOrNull())
    }

    @Test
    fun `playlist page exposes the next offset only when it moves forward`() {
        val items = listOf(MediaItemDto(mediaid = "m"))
        val next = PlaylistResponseDto(
            links = LinksDto(next = "https://x/v2/playlists/P?page_limit=10&page_offset=11"),
            playlist = items,
        )
        assertEquals(11, next.toPlaylistPage("P", currentOffset = 1).nextOffset)
        assertNull(next.toPlaylistPage("P", currentOffset = 11).nextOffset)

        val last = PlaylistResponseDto(links = LinksDto(next = null), playlist = items)
        assertNull(last.toPlaylistPage("P", currentOffset = 1).nextOffset)

        val emptyWithNext = next.copy(playlist = emptyList())
        assertNull(emptyWithNext.toPlaylistPage("P", currentOffset = 1).nextOffset)
    }

    @Test
    fun `playlist uses feed id and falls back to requested id`() {
        val withFeed = PlaylistResponseDto(feedid = "P1", title = "Cine", playlist = listOf(MediaItemDto(mediaid = "m")))
        assertEquals("P1", withFeed.toPlaylist("REQ").id)
        assertEquals(1, withFeed.toPlaylist("REQ").videos.size)
        assertEquals("REQ", PlaylistResponseDto().toPlaylist("REQ").id)
    }
}
