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
    fun provideOkHttpClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(okhttp3.Cache(java.io.File(context.cacheDir, "http_cache"), 20L * 1024 * 1024))
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

    fun createJwPlayerApi(baseUrl: String, client: OkHttpClient = OkHttpClient()): JwPlayerApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JwPlayerApi::class.java)
}
