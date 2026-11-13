package `in`.sms.tringapps.library.domain.usecase

import `in`.sms.tringapps.testing.FakeLibraryRepository
import `in`.sms.tringapps.testing.testVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryUseCasesTest {

    private val repository = FakeLibraryRepository()
    private val video = testVideo("v1")
    private val save = SaveWatchProgressUseCase(repository)
    private val resume = GetResumePositionUseCase(repository)

    @Test
    fun `progress in the middle is stored and resumable`() = runTest {
        save(video, positionMs = 60_000, durationMs = 600_000)

        assertEquals(60_000L, resume("v1"))
        assertEquals(listOf("v1"), ObserveContinueWatchingUseCase(repository)().first().map { it.video.mediaId })
    }

    @Test
    fun `progress before ten seconds is ignored`() = runTest {
        save(video, positionMs = 5_000, durationMs = 600_000)
        assertTrue(repository.continueWatching.value.isEmpty())
        assertEquals(0L, resume("v1"))
    }

    @Test
    fun `finishing a title removes it from continue watching`() = runTest {
        save(video, positionMs = 60_000, durationMs = 600_000)
        save(video, positionMs = 580_000, durationMs = 600_000)

        assertTrue(repository.continueWatching.value.isEmpty())
        assertEquals(0L, resume("v1"))
    }

    @Test
    fun `unknown duration still stores progress`() = runTest {
        save(video, positionMs = 30_000, durationMs = 0)
        assertEquals(30_000L, resume("v1"))
    }

    @Test
    fun `resume of an unknown title starts at zero`() = runTest {
        assertEquals(0L, resume("nope"))
    }

    @Test
    fun `toggle adds then removes from my list`() = runTest {
        val toggle = ToggleMyListUseCase(repository)

        assertTrue(toggle(video))
        assertEquals(listOf("v1"), ObserveMyListUseCase(repository)().first().map { it.mediaId })
        assertFalse(toggle(video))
        assertTrue(repository.myList.value.isEmpty())
    }
}
