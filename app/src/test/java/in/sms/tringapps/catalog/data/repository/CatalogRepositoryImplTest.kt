package `in`.sms.tringapps.catalog.data.repository

import androidx.paging.testing.asSnapshot
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.repository.ContentNotFoundException
import `in`.sms.tringapps.core.network.NetworkModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogRepositoryImplTest {

    private val server = MockWebServer()

    /** Keyed by path + query, e.g. `/v2/playlists/P?page_offset=1&page_limit=20`. */
    private val responses = mutableMapOf<String, MockResponse>()
    private val requested = mutableListOf<String>()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.url
                val target = url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: "")
                synchronized(requested) { requested += target }
                return responses[target] ?: MockResponse.Builder().code(404).build()
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private fun TestScope.repository() = CatalogRepositoryImpl(
        api = NetworkModule.createJwPlayerApi(server.url("/").toString()),
        configId = CONFIG_ID,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun respond(target: String, body: String) {
        responses[target] = MockResponse.Builder().code(200).body(body).build()
    }

    private fun page(id: String, offset: Int, limit: Int) = "/v2/playlists/$id?page_offset=$offset&page_limit=$limit"

    @Test
    fun `getSections requests the config and maps sections`() = runTest {
        respond("/apps/configs/$CONFIG_ID.json", CONFIG_JSON)

        val sections = repository().getSections().getOrThrow()

        assertEquals(listOf("/apps/configs/$CONFIG_ID.json"), requested)
        assertEquals(listOf("lPKrWxbW", "SOSjtTUH", "Yq6jhuQ8"), sections.map { it.playlistId })
        assertTrue(sections[0].featured)
        assertEquals(SectionType.CONTINUE_WATCHING, sections[1].type)
        assertEquals("Novedades en cine", sections[2].title)
    }

    @Test
    fun `getPlaylistPage sends offset and limit and maps items and next offset`() = runTest {
        respond(page("Yq6jhuQ8", 1, 2), playlistJson(next = 3))

        val result = repository().getPlaylistPage("Yq6jhuQ8", offset = 1, limit = 2).getOrThrow()

        assertEquals(listOf(page("Yq6jhuQ8", 1, 2)), requested)
        assertEquals(3, result.nextOffset)
        val playlist = result.playlist
        assertEquals("Novedades en cine", playlist.title)
        assertEquals(listOf("YjclsUIC", "E6DMpaUF"), playlist.videos.map { it.mediaId })
        val first = playlist.videos.first()
        assertEquals(5566L, first.durationSeconds)
        assertEquals("https://cdn.jwplayer.com/manifests/YjclsUIC.m3u8", first.stream?.url)
        assertEquals(true, first.stream?.isHls)
        assertEquals("https://cdn.jwplayer.com/v2/media/YjclsUIC/poster.jpg?width=640", first.thumbnailUrl)
        assertFalse(playlist.videos[1].isPlayable)
    }

    @Test
    fun `pages are cached per playlist offset and limit`() = runTest {
        respond(page("P", 1, 2), playlistJson(next = null))
        val repository = repository()

        repository.getPlaylistPage("P", 1, 2)
        repository.getPlaylistPage("P", 1, 2)

        assertEquals(1, requested.size)
    }

    @Test
    fun `paged playlist walks every page`() = runTest {
        respond(page("P", 1, 2), playlistJson(next = 3))
        respond(page("P", 3, 2), playlistJson(next = null, idPrefix = "b"))

        val items = repository().pagedPlaylist("P", pageSize = 2).asSnapshot { appendScrollWhile { true } }

        assertEquals(listOf("YjclsUIC", "E6DMpaUF", "bYjclsUIC", "bE6DMpaUF"), items.map { it.mediaId })
        assertEquals(listOf(page("P", 1, 2), page("P", 3, 2)), requested)
    }

    @Test
    fun `last page has no next offset`() = runTest {
        respond(page("P", 1, 2), playlistJson(next = null))
        assertNull(repository().getPlaylistPage("P", 1, 2).getOrThrow().nextOffset)
    }

    @Test
    fun `clearCache forces the next request to hit the network`() = runTest {
        respond(page("P", 1, 25), playlistJson(next = null))
        val repository = repository()
        repository.getPlaylistPage("P", 1, 25)

        repository.clearCache()
        repository.getPlaylistPage("P", 1, 25)

        assertEquals(2, requested.size)
        assertTrue(repository.cachedVideos().isNotEmpty())
    }

    @Test
    fun `paging requests the next page only when JW advertises one`() = runTest {
        respond(page("P", 1, 25), playlistJson(next = null))

        val items = repository().pagedPlaylist("P", pageSize = 25).asSnapshot { appendScrollWhile { true } }

        assertEquals(2, items.size)
        assertEquals(listOf(page("P", 1, 25)), requested)
    }

    @Test
    fun `getVideo is served from cache after its page was loaded`() = runTest {
        respond(page("P", 1, 2), playlistJson(next = null))
        val repository = repository()
        repository.getPlaylistPage("P", 1, 2).getOrThrow()

        val video = repository.getVideo("YjclsUIC").getOrThrow()

        assertEquals("Almas pasajeras", video.title)
        assertEquals(1, requested.size)
        assertEquals(listOf("YjclsUIC", "E6DMpaUF"), repository.cachedVideos().map { it.mediaId })
    }

    @Test
    fun `getVideo fetches the media endpoint when not cached`() = runTest {
        respond("/v2/media/YjclsUIC", playlistJson(next = null))

        val video = repository().getVideo("YjclsUIC").getOrThrow()

        assertEquals("YjclsUIC", video.mediaId)
        assertEquals(listOf("/v2/media/YjclsUIC"), requested)
    }

    @Test
    fun `getVideo with an empty media response is not found`() = runTest {
        respond("/v2/media/gone", """{"playlist": []}""")
        assertTrue(repository().getVideo("gone").exceptionOrNull() is ContentNotFoundException)
    }

    @Test
    fun `http error becomes a failed result`() = runTest {
        assertTrue(repository().getPlaylistPage("missing", 1, 20).exceptionOrNull() is HttpException)
    }

    @Test
    fun `malformed json becomes a failed result`() = runTest {
        respond(page("bad", 1, 20), "{not json")
        assertTrue(repository().getPlaylistPage("bad", 1, 20).isFailure)
    }

    private companion object {
        const val CONFIG_ID = "kxqzxtpq"

        val CONFIG_JSON = """
            {"siteName": "Test", "styling": {"backgroundColor": "#111111"},
             "content": [
               {"contentId": "lPKrWxbW", "custom": {}, "featured": true, "title": "Destacados", "type": "playlist"},
               {"contentId": "SOSjtTUH", "custom": {"title-en": "Continue Watching"}, "featured": false, "title": "Continuar viendo", "type": "continue_watching"},
               {"contentId": "Yq6jhuQ8", "featured": false, "title": "Novedades en cine", "type": "playlist"}
             ]}
        """.trimIndent()

        fun playlistJson(next: Int?, idPrefix: String = ""): String {
            val nextLink = next?.let { ""","next": "https://cdn.jwplayer.com/v2/playlists/Yq6jhuQ8?page_limit=2&page_offset=$it&format=json"""" }.orEmpty()
            return """
            {"title": "Novedades en cine", "feedid": "Yq6jhuQ8", "kind": "MANUAL",
             "links": {"first": "https://cdn.jwplayer.com/v2/playlists/Yq6jhuQ8?page_limit=2&page_offset=1"$nextLink},
             "playlist": [
               {"title": "Almas pasajeras", "mediaid": "${idPrefix}YjclsUIC",
                "image": "https://cdn.jwplayer.com/v2/media/YjclsUIC/poster.jpg?width=720",
                "images": [
                  {"src": "https://cdn.jwplayer.com/v2/media/YjclsUIC/poster.jpg?width=320", "width": 320, "type": "image/jpeg"},
                  {"src": "https://cdn.jwplayer.com/v2/media/YjclsUIC/poster.jpg?width=640", "width": 640, "type": "image/jpeg"},
                  {"src": "https://cdn.jwplayer.com/v2/media/YjclsUIC/poster.jpg?width=1280", "width": 1280, "type": "image/jpeg"}
                ],
                "duration": 5566, "description": "Drama", "agerating": "13+", "pubdate": 1786108020,
                "sources": [
                  {"file": "https://cdn.jwplayer.com/manifests/YjclsUIC.m3u8", "type": "application/vnd.apple.mpegurl"},
                  {"file": "https://cdn.jwplayer.com/videos/YjclsUIC-YQMW3lWr.m4a", "type": "audio/mp4", "label": "AAC Audio"},
                  {"file": "https://cdn.jwplayer.com/videos/YjclsUIC-Z4EE9Wa0.mp4", "type": "video/mp4", "height": 180, "width": 320, "label": "180p"}
                ]},
               {"title": "Lola Flores", "mediaid": "${idPrefix}E6DMpaUF", "duration": 0,
                "sources": [{"file": "None", "type": "application/octet-stream"}], "variations": {}}
             ]}
            """.trimIndent()
        }
    }
}
