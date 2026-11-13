package `in`.sms.tringapps.core.network

import `in`.sms.tringapps.core.network.dto.AppConfigDto
import `in`.sms.tringapps.core.network.dto.PlaylistResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** JW Player delivery APIs (public, no auth). */
interface JwPlayerApi {

    /** App config listing the content sections (shelves) of the app. */
    @GET("apps/configs/{configId}.json")
    suspend fun getAppConfig(@Path("configId") configId: String): AppConfigDto

    /**
     * One page of a playlist. [pageOffset] is the 1-based index of the first item,
     * so page N of size L starts at `1 + (N - 1) * L`.
     */
    @GET("v2/playlists/{playlistId}")
    suspend fun getPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("page_offset") pageOffset: Int,
        @Query("page_limit") pageLimit: Int,
    ): PlaylistResponseDto

    /** Single media item; JW returns it wrapped in the same playlist envelope. */
    @GET("v2/media/{mediaId}")
    suspend fun getMedia(@Path("mediaId") mediaId: String): PlaylistResponseDto
}
