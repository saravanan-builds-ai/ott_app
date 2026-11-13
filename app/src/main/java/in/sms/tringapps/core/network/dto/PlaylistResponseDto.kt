package `in`.sms.tringapps.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistResponseDto(
    val feedid: String? = null,
    val title: String? = null,
    val description: String? = null,
    val links: LinksDto? = null,
    val playlist: List<MediaItemDto> = emptyList(),
)

/** JW pagination links; [next] is absent on the last page. */
@Serializable
data class LinksDto(
    val first: String? = null,
    val next: String? = null,
    val last: String? = null,
)

@Serializable
data class MediaItemDto(
    val mediaid: String? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val images: List<ImageDto> = emptyList(),
    val duration: Double? = null,
    val sources: List<SourceDto> = emptyList(),
    val agerating: String? = null,
)

@Serializable
data class ImageDto(
    val src: String? = null,
    val width: Int? = null,
)

@Serializable
data class SourceDto(
    val file: String? = null,
    val type: String? = null,
    val height: Int? = null,
)
