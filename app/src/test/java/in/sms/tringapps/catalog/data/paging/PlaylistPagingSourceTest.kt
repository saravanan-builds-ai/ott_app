package `in`.sms.tringapps.catalog.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.testing.testPlaylist
import `in`.sms.tringapps.testing.testVideo
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPagingSourceTest {

    private val requests = mutableListOf<Pair<Int, Int>>()
    private val config = PagingConfig(pageSize = 2, initialLoadSize = 2, enablePlaceholders = false)

    /** 5 items served in pages of 2 with 1-based offsets, like JW. */
    private fun source(failAt: Int? = null) = PlaylistPagingSource { offset, limit ->
        requests += offset to limit
        if (offset == failAt) return@PlaylistPagingSource Result.failure(IOException())
        val all = (1..5).map { testVideo("v$it") }
        val page = all.drop(offset - 1).take(limit)
        val next = (offset + limit).takeIf { it <= all.size }
        Result.success(PlaylistPage(testPlaylist("P", *page.toTypedArray()), next))
    }

    @Test
    fun `first load starts at offset one`() = runTest {
        val page = TestPager(config, source()).refresh() as PagingSource.LoadResult.Page

        assertEquals(listOf("v1", "v2"), page.data.map { it.mediaId })
        assertEquals(3, page.nextKey)
        assertNull(page.prevKey)
        assertEquals(1 to 2, requests.first())
    }

    @Test
    fun `appends follow next offsets until the end`() = runTest {
        val pager = TestPager(config, source())
        pager.refresh()
        pager.append()
        val last = pager.append() as PagingSource.LoadResult.Page

        assertEquals(listOf("v5"), last.data.map { it.mediaId })
        assertNull(last.nextKey)
        assertEquals(listOf(1, 3, 5), requests.map { it.first })
        assertEquals(5, pager.getPages().flatMap { it.data }.size)
    }

    @Test
    fun `failure becomes a load error`() = runTest {
        val pager = TestPager(config, source(failAt = 3))
        pager.refresh()

        assertTrue(pager.append() is PagingSource.LoadResult.Error)
    }
}
