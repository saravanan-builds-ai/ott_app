package `in`.sms.tringapps.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.sms.tringapps.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** The JW Player app-config id the home screen is built from. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JwConfigId

/** The JW playlist shown as the "Trending Now" Top 10. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JwTrendingPlaylistId

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideJwPlayerApi(client: OkHttpClient): JwPlayerApi =
        createJwPlayerApi(BuildConfig.JW_BASE_URL, client)

    @Provides
    @JwConfigId
    fun provideConfigId(): String = BuildConfig.JW_CONFIG_ID

    @Provides
    @JwTrendingPlaylistId
    fun provideTrendingPlaylistId(): String = BuildConfig.JW_TRENDING_PLAYLIST_ID

    fun createJwPlayerApi(baseUrl: String, client: OkHttpClient = OkHttpClient()): JwPlayerApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JwPlayerApi::class.java)
}
