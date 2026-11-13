package `in`.sms.tringapps.library.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import `in`.sms.tringapps.testing.testVideo
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryImplTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private var now = 1_000L

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        produceFile = { File(tmp.root, "library.preferences_pb") },
    )

    private fun TestScope.repository(store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = dataStore()) =
        LibraryRepositoryImpl(store) { now }

    @Test
    fun `saved progress round-trips with the video snapshot`() = runTest {
        val repository = repository()
        val video = testVideo("v1", title = "Almas")

        repository.saveProgress(video, positionMs = 42_000, durationMs = 600_000)

        val item = repository.getProgress("v1")!!
        assertEquals(video, item.video)
        assertEquals(42_000L, item.positionMs)
        assertEquals(600_000L, item.durationMs)
        assertEquals(1_000L, item.updatedAtMs)
        assertEquals(0.07f, item.progress, 0.0001f)
    }

    @Test
    fun `continue watching is most recent first without duplicates`() = runTest {
        val repository = repository()
        repository.saveProgress(testVideo("a"), 20_000, 100_000)
        now = 2_000
        repository.saveProgress(testVideo("b"), 20_000, 100_000)
        now = 3_000
        repository.saveProgress(testVideo("a"), 30_000, 100_000)

        val items = repository.observeContinueWatching().first()

        assertEquals(listOf("a", "b"), items.map { it.video.mediaId })
        assertEquals(30_000L, items.first().positionMs)
    }

    @Test
    fun `continue watching keeps at most twenty titles`() = runTest {
        val repository = repository()
        repeat(25) { repository.saveProgress(testVideo("v$it"), 20_000, 100_000) }

        val items = repository.observeContinueWatching().first()

        assertEquals(20, items.size)
        assertEquals("v24", items.first().video.mediaId)
    }

    @Test
    fun `remove progress deletes only that title`() = runTest {
        val repository = repository()
        repository.saveProgress(testVideo("a"), 20_000, 100_000)
        repository.saveProgress(testVideo("b"), 20_000, 100_000)

        repository.removeProgress("a")

        assertNull(repository.getProgress("a"))
        assertEquals(listOf("b"), repository.observeContinueWatching().first().map { it.video.mediaId })
    }

    @Test
    fun `my list toggles and lists newest first`() = runTest {
        val repository = repository()

        assertTrue(repository.toggleMyList(testVideo("a")))
        assertTrue(repository.toggleMyList(testVideo("b")))
        assertEquals(listOf("b", "a"), repository.observeMyList().first().map { it.mediaId })

        assertFalse(repository.toggleMyList(testVideo("a")))
        assertEquals(listOf("b"), repository.observeMyList().first().map { it.mediaId })
    }

    @Test
    fun `corrupt stored payload is treated as empty`() = runTest {
        val store = dataStore()
        store.edit { it[stringPreferencesKey("my_list")] = "{broken" }

        val repository = repository(store)

        assertTrue(repository.observeMyList().first().isEmpty())
        assertTrue(repository.toggleMyList(testVideo("a")))
    }
}
