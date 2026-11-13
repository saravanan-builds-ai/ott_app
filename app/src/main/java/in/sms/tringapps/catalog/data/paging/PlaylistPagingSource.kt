package `in`.sms.tringapps.catalog.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.catalog.domain.model.Video

/**
 * Pages a JW playlist with `page_offset` (1-based item index) / `page_limit`.
 * The key is the offset of the page to load; the next key only exists when JW's
 * `links.next` says there is another page. Items already emitted by an earlier page are
 * dropped so lazy lists can key by media id.
 */
class PlaylistPagingSource(
    private val loadPage: suspend (offset: Int, limit: Int) -> Result<PlaylistPage>,
) : PagingSource<Int, Video>() {

    private val seenIds = mutableSetOf<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Video> {
        val offset = params.key ?: FIRST_OFFSET
        return loadPage(offset, params.loadSize).fold(
            onSuccess = { page ->
                val fresh = synchronized(seenIds) { page.playlist.videos.filter { seenIds.add(it.mediaId) } }
                LoadResult.Page(data = fresh, prevKey = null, nextKey = page.nextOffset)
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    /** Always restart from the top on refresh; pages are cached by the repository. */
    override fun getRefreshKey(state: PagingState<Int, Video>): Int? = null

    companion object {
        const val FIRST_OFFSET = 1
    }
}
