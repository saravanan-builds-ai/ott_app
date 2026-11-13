package `in`.sms.tringapps.catalog.domain.usecase

import `in`.sms.tringapps.testing.FakeCatalogRepository
import `in`.sms.tringapps.testing.testVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCatalogUseCaseTest {

    private val repository = FakeCatalogRepository().apply {
        cached = listOf(
            testVideo("1", title = "Un perro andaluz"),
            testVideo("2", title = "Almas pasajeras"),
            testVideo("3", title = "Película sin título"),
            testVideo("4", title = "Andalucía secreta"),
            testVideo("5", title = "Hidden", playable = false),
            testVideo("1", title = "Un perro andaluz"),
        )
    }
    private val search = SearchCatalogUseCase(repository)

    @Test
    fun `blank query returns nothing`() {
        assertTrue(search("   ").isEmpty())
    }

    @Test
    fun `matching is case and accent insensitive`() {
        assertEquals(listOf("3"), search("PELICULA").map { it.mediaId })
    }

    @Test
    fun `title prefix ranks above word prefix and contains`() {
        // "4" starts with "andal", "1" has a word starting with it.
        assertEquals(listOf("4", "1"), search("andal").map { it.mediaId })
    }

    @Test
    fun `description matches come last`() {
        val results = search("description 2")
        assertEquals(listOf("2"), results.map { it.mediaId })
    }

    @Test
    fun `unplayable and duplicate items are excluded`() {
        assertTrue(search("hidden").isEmpty())
        assertEquals(1, search("perro").size)
    }

    @Test
    fun `suggestions are unique playable cached items`() {
        assertEquals(listOf("1", "2", "3", "4"), search.suggestions().map { it.mediaId })
    }
}
