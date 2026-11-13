package `in`.sms.tringapps.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.usecase.GetVideoUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveContinueWatchingUseCase
import `in`.sms.tringapps.library.domain.usecase.ObserveMyListUseCase
import `in`.sms.tringapps.library.domain.usecase.ToggleMyListUseCase
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Error(val kind: DetailsError) : DetailsUiState
    data class Content(
        val video: Video,
        val isInMyList: Boolean,
        /** 0..1 when the title was started before (shows "Resume"), otherwise null. */
        val resumeProgress: Float?,
    ) : DetailsUiState
}

enum class DetailsError { NETWORK, NOT_FOUND }

/**
 * Details for one title. Only the media id travels through navigation; the video (and its
 * playback URL) is resolved from the repository cache, or the JW media endpoint after
 * process death.
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getVideo: GetVideoUseCase,
    observeMyList: ObserveMyListUseCase,
    observeContinueWatching: ObserveContinueWatchingUseCase,
    private val toggleMyListUseCase: ToggleMyListUseCase,
) : ViewModel() {

    private val mediaId: String? = savedStateHandle.get<String>(ARG_MEDIA_ID)

    /** null while loading. */
    private val videoResult = MutableStateFlow<Result<Video>?>(null)

    val uiState: StateFlow<DetailsUiState> =
        combine(videoResult, observeMyList(), observeContinueWatching()) { result, myList, progress ->
            when {
                result == null -> DetailsUiState.Loading
                result.isFailure -> DetailsUiState.Error(
                    if (result.exceptionOrNull() is IOException) DetailsError.NETWORK else DetailsError.NOT_FOUND,
                )
                else -> {
                    val video = result.getOrThrow()
                    DetailsUiState.Content(
                        video = video,
                        isInMyList = myList.any { it.mediaId == video.mediaId },
                        resumeProgress = progress.firstOrNull { it.video.mediaId == video.mediaId }?.progress,
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DetailsUiState.Loading)

    init {
        load()
    }

    fun retry() = load()

    fun toggleMyList() {
        val video = videoResult.value?.getOrNull() ?: return
        viewModelScope.launch { toggleMyListUseCase(video) }
    }

    private fun load() {
        val id = mediaId
        if (id.isNullOrBlank()) {
            videoResult.value = Result.failure(IllegalArgumentException("Missing media id"))
            return
        }
        videoResult.value = null
        viewModelScope.launch { videoResult.value = getVideo(id) }
    }

    companion object {
        /** Must match the property of [in.sms.tringapps.feature.details.navigation.DetailsDestination]. */
        const val ARG_MEDIA_ID = "mediaId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
