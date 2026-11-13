package `in`.sms.tringapps.catalog.domain.usecase

import androidx.paging.PagingData
import androidx.paging.filter
import `in`.sms.tringapps.catalog.domain.model.HomeLayout
import `in`.sms.tringapps.catalog.domain.model.Playlist
import `in`.sms.tringapps.catalog.domain.model.SectionType
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.repository.CatalogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Page size for the first page of every rail and for each further page (JW `page_limit`). */
const val PLAYLIST_PAGE_SIZE = 25

/**
 * Builds the home layout from the config: playlist sections (de-duplicated, config order),
 * the featured playlist, and the special user sections, which are handled on-device.
 * Nothing about sections is hardcoded; everything comes from the config.
 */
class GetHomeContentUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<HomeLayout> {
        if (forceRefresh) repository.clearCache()
        return repository.getSections().map { sections ->
            val playlists = sections
                .filter { it.type == SectionType.PLAYLIST }
                .distinctBy { it.playlistId }
            HomeLayout(
                playlists = playlists,
                featuredPlaylistId = playlists.firstOrNull { it.featured }?.playlistId,
                continueWatching = sections.firstOrNull { it.type == SectionType.CONTINUE_WATCHING },
                favorites = sections.firstOrNull { it.type == SectionType.FAVORITES },
            )
        }
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

class GetVideoUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(mediaId: String): Result<Video> = repository.getVideo(mediaId)
}
