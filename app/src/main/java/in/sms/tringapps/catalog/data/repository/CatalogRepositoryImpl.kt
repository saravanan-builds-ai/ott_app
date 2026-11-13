package `in`.sms.tringapps.catalog.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import `in`.sms.tringapps.catalog.data.mapper.toPlaylistPage
import `in`.sms.tringapps.catalog.data.mapper.toSections
import `in`.sms.tringapps.catalog.data.mapper.toVideoOrNull
import `in`.sms.tringapps.catalog.data.paging.PlaylistPagingSource
import `in`.sms.tringapps.catalog.domain.model.PlaylistPage
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.repository.CatalogRepository
import `in`.sms.tringapps.catalog.domain.repository.ContentNotFoundException
import `in`.sms.tringapps.core.common.IoDispatcher
import `in`.sms.tringapps.core.common.suspendRunCatching
import `in`.sms.tringapps.core.network.JwConfigId
import `in`.sms.tringapps.core.network.JwPlayerApi
import `in`.sms.tringapps.core.network.JwTrendingPlaylistId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val api: JwPlayerApi,
    @param:JwConfigId private val configId: String,
    @param:JwTrendingPlaylistId private val trendingPlaylistId: String,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CatalogRepository {

    private data class PageKey(val playlistId: String, val offset: Int, val limit: Int)

    /** Pages already fetched this session; lets rails' paging reuse the home preview request. */
    private val pageCache = ConcurrentHashMap<PageKey, PlaylistPage>()

    /** Videos seen in any page (insertion-ordered for search suggestions). */
    private val videoCache = java.util.Collections.synchronizedMap(LinkedHashMap<String, Video>())

    override suspend fun getSections(): Result<List<Section>> = withContext(ioDispatcher) {
        suspendRunCatching { api.getAppConfig(configId).toSections() }
    }

    override suspend fun getPlaylistPage(playlistId: String, offset: Int, limit: Int): Result<PlaylistPage> {
        val key = PageKey(playlistId, offset, limit)
        pageCache[key]?.let { return Result.success(it) }
        return withContext(ioDispatcher) {
            suspendRunCatching {
                api.getPlaylist(playlistId, offset, limit)
                    .toPlaylistPage(playlistId, offset)
                    .also { page ->
                        pageCache[key] = page
                        page.playlist.videos.forEach { videoCache.putIfAbsent(it.mediaId, it) }
                    }
            }
        }
    }

    override fun pagedPlaylist(playlistId: String, pageSize: Int): Flow<PagingData<Video>> =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                prefetchDistance = pageSize / 2,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                PlaylistPagingSource { offset, _ -> getPlaylistPage(playlistId, offset, pageSize) }
            },
        ).flow

    override suspend fun getTrending(limit: Int): Result<List<Video>> =
        getPlaylistPage(trendingPlaylistId, offset = 1, limit = limit).map { it.playlist.videos }

    override suspend fun getVideo(mediaId: String): Result<Video> {
        videoCache[mediaId]?.let { return Result.success(it) }
        return withContext(ioDispatcher) {
            suspendRunCatching {
                api.getMedia(mediaId).playlist.firstNotNullOfOrNull { it.toVideoOrNull() }
                    ?.also { videoCache[mediaId] = it }
                    ?: throw ContentNotFoundException(mediaId)
            }
        }
    }

    override fun cachedVideos(): List<Video> = synchronized(videoCache) { videoCache.values.toList() }
}
