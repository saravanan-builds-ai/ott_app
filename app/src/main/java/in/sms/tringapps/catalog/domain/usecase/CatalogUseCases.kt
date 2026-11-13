package `in`.sms.tringapps.catalog.domain.usecase

import androidx.paging.PagingData
import androidx.paging.filter
import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.Section
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.repository.CatalogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Page size used by rails and the "See all" grid. */
const val PLAYLIST_PAGE_SIZE = 20
const val TRENDING_SIZE = 10

/**
 * Sections that can be browsed on the home screen: only real playlists
 * (user-specific shelves like "continue watching" / "favorites" are handled on-device),
 * de-duplicated by playlist id, in config order.
 */
class GetHomeSectionsUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(): Result<List<Section>> =
        repository.getSections().map { sections ->
            sections
                .filter { it.type == SectionType.PLAYLIST }
                .distinctBy { it.playlistId }
        }
}

/** First page of a playlist, keeping only items that can actually be streamed. */
class GetPlayablePlaylistUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(playlistId: String, limit: Int = PLAYLIST_PAGE_SIZE): Result<Playlist> =
        repository.getPlaylistPage(playlistId, offset = 1, limit = limit).map { page ->
            page.playlist.copy(videos = page.playlist.videos.filter(Video::isPlayable))
        }
}

/** Endless, playable-only paging over a playlist. */
class GetPagedPlaylistUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    operator fun invoke(playlistId: String): Flow<PagingData<Video>> =
        repository.pagedPlaylist(playlistId, PLAYLIST_PAGE_SIZE)
            .map { data -> data.filter { it.isPlayable } }
}

/** Ranked "Trending Now" Top 10. */
class GetTrendingUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(): Result<List<Video>> =
        repository.getTrending(limit = PLAYLIST_PAGE_SIZE).map { videos ->
            videos.filter(Video::isPlayable).distinctBy { it.mediaId }.take(TRENDING_SIZE)
        }
}

class GetVideoUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(mediaId: String): Result<Video> = repository.getVideo(mediaId)
}
