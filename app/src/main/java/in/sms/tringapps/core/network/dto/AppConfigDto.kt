package `in`.sms.tringapps.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(
    val siteName: String? = null,
    val content: List<ContentDto> = emptyList(),
)

@Serializable
data class ContentDto(
    val contentId: String? = null,
    val title: String? = null,
    val type: String? = null,
    val featured: Boolean = false,
)
