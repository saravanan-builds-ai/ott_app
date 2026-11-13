package `in`.sms.tringapps.feature.player.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.usecase.GetVideoUseCase
import `in`.sms.tringapps.core.common.ApplicationScope
import `in`.sms.tringapps.library.domain.usecase.GetResumePositionUseCase
import `in`.sms.tringapps.library.domain.usecase.SaveWatchProgressUseCase
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getVideo: GetVideoUseCase,
    private val getResumePosition: GetResumePositionUseCase,
    private val saveWatchProgress: SaveWatchProgressUseCase,
    /** Progress writes must survive the screen closing, so they don't use viewModelScope. */
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val mediaId: String? = savedStateHandle.get<String>(ARG_MEDIA_ID)

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var lastPersistedPositionMs = Long.MIN_VALUE

    init {
        load()
    }

    fun retry() = load()

    /**
     * Called periodically by the player and once more when it is released ([isFinal]).
     * The position is always kept for retry/process death; it is written to Continue
     * Watching every [PERSIST_INTERVAL_MS] and on close.
     */
    fun onProgress(positionMs: Long, durationMs: Long, isFinal: Boolean = false) {
        val position = positionMs.coerceAtLeast(0L)
        savedStateHandle[KEY_POSITION] = position
        val video = (_uiState.value as? PlayerUiState.Ready)?.video ?: return
        if (isFinal || abs(position - lastPersistedPositionMs) >= PERSIST_INTERVAL_MS) {
            lastPersistedPositionMs = position
            persist(video, position, durationMs)
        }
    }

    fun onPlaybackError() {
        _uiState.value = PlayerUiState.Error(PlayerError.PLAYBACK)
    }

    private fun persist(video: Video, positionMs: Long, durationMs: Long) {
        val duration = if (durationMs > 0) durationMs else video.durationSeconds * 1000
        appScope.launch { saveWatchProgress(video, positionMs, duration) }
    }

    private fun load() {
        val id = mediaId
        if (id.isNullOrBlank()) {
            _uiState.value = PlayerUiState.Error(PlayerError.NOT_FOUND)
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            val result = getVideo(id)
            _uiState.value = result.fold(
                onSuccess = { video ->
                    if (video.isPlayable) {
                        val start = savedStateHandle.get<Long>(KEY_POSITION) ?: getResumePosition(id)
                        lastPersistedPositionMs = start
                        PlayerUiState.Ready(video, start)
                    } else {
                        PlayerUiState.Error(PlayerError.NOT_PLAYABLE)
                    }
                },
                onFailure = { error ->
                    // Connectivity problems are retryable; anything else (HTTP 404,
                    // ContentNotFoundException, malformed payload) means the item is unavailable.
                    PlayerUiState.Error(
                        if (error is IOException) PlayerError.NETWORK else PlayerError.NOT_FOUND,
                    )
                },
            )
        }
    }

    companion object {
        /** Must match the property name of [in.sms.tringapps.feature.player.navigation.PlayerDestination]. */
        const val ARG_MEDIA_ID = "mediaId"
        const val PERSIST_INTERVAL_MS = 10_000L
        private const val KEY_POSITION = "playback_position_ms"
    }
}
