package `in`.sms.tringapps.catalog.domain.usecase

import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.repository.CatalogRepository
import java.text.Normalizer
import javax.inject.Inject

/**
 * On-device search over everything loaded this session (the JW delivery API for this
 * property has no search endpoint). Accent- and case-insensitive; title matches rank
 * above description matches.
 */
class SearchCatalogUseCase @Inject constructor(
    private val repository: CatalogRepository,
) {
    operator fun invoke(query: String, limit: Int = MAX_RESULTS): List<Video> {
        val needle = query.normalized()
        if (needle.isEmpty()) return emptyList()
        return playable()
            .mapNotNull { video ->
                val title = video.title.normalized()
                val rank = when {
                    title.startsWith(needle) -> 0
                    title.split(' ').any { it.startsWith(needle) } -> 1
                    title.contains(needle) -> 2
                    video.description.normalized().contains(needle) -> 3
                    else -> return@mapNotNull null
                }
                rank to video
            }
            .sortedBy { it.first }
            .map { it.second }
            .take(limit)
    }

    /** Shown before the user types anything. */
    fun suggestions(limit: Int = MAX_SUGGESTIONS): List<Video> = playable().take(limit)

    private fun playable() = repository.cachedVideos().filter(Video::isPlayable).distinctBy { it.mediaId }

    private fun String.normalized(): String =
        Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace(WHITESPACE, " ")

    private companion object {
        const val MAX_RESULTS = 60
        const val MAX_SUGGESTIONS = 18
        val DIACRITICS = "\\p{Mn}+".toRegex()
        val WHITESPACE = "\\s+".toRegex()
    }
}
