package `in`.sms.tringapps.library.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.core.common.TimeProvider
import `in`.sms.tringapps.library.domain.model.ContinueWatchingItem
import `in`.sms.tringapps.library.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Library persisted in Preferences DataStore as two small JSON lists. */
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider,
) : LibraryRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val progressSerializer = ListSerializer(StoredProgress.serializer())
    private val myListSerializer = ListSerializer(StoredMyListEntry.serializer())

    override fun observeContinueWatching(): Flow<List<ContinueWatchingItem>> =
        dataStore.data
            .map { prefs -> prefs.decode(PROGRESS_KEY, progressSerializer).map { it.toItem() } }
            .distinctUntilChanged()

    override fun observeMyList(): Flow<List<Video>> =
        dataStore.data
            .map { prefs -> prefs.decode(MY_LIST_KEY, myListSerializer).map { it.video.toVideo() } }
            .distinctUntilChanged()

    override suspend fun getProgress(mediaId: String): ContinueWatchingItem? =
        dataStore.data.first().decode(PROGRESS_KEY, progressSerializer)
            .firstOrNull { it.video.mediaId == mediaId }
            ?.toItem()

    override suspend fun saveProgress(video: Video, positionMs: Long, durationMs: Long) {
        dataStore.edit { prefs ->
            val entry = StoredProgress(video.toStored(), positionMs, durationMs, timeProvider.nowMillis())
            val updated = listOf(entry) + prefs.decode(PROGRESS_KEY, progressSerializer)
                .filterNot { it.video.mediaId == video.mediaId }
            prefs.encode(PROGRESS_KEY, progressSerializer, updated.take(MAX_CONTINUE_WATCHING))
        }
    }

    override suspend fun removeProgress(mediaId: String) {
        dataStore.edit { prefs ->
            val current = prefs.decode(PROGRESS_KEY, progressSerializer)
            prefs.encode(PROGRESS_KEY, progressSerializer, current.filterNot { it.video.mediaId == mediaId })
        }
    }

    override suspend fun toggleMyList(video: Video): Boolean {
        var added = false
        dataStore.edit { prefs ->
            val current = prefs.decode(MY_LIST_KEY, myListSerializer)
            val updated = if (current.any { it.video.mediaId == video.mediaId }) {
                current.filterNot { it.video.mediaId == video.mediaId }
            } else {
                added = true
                listOf(StoredMyListEntry(video.toStored(), timeProvider.nowMillis())) + current
            }
            prefs.encode(MY_LIST_KEY, myListSerializer, updated)
        }
        return added
    }

    /** Corrupt or legacy payloads are treated as empty rather than crashing the app. */
    private fun <T> Preferences.decode(key: Preferences.Key<String>, serializer: KSerializer<List<T>>): List<T> =
        this[key]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    private fun <T> MutablePreferences.encode(
        key: Preferences.Key<String>,
        serializer: KSerializer<List<T>>,
        value: List<T>,
    ) {
        this[key] = json.encodeToString(serializer, value)
    }

    private companion object {
        val PROGRESS_KEY = stringPreferencesKey("continue_watching")
        val MY_LIST_KEY = stringPreferencesKey("my_list")
        const val MAX_CONTINUE_WATCHING = 20
    }
}
