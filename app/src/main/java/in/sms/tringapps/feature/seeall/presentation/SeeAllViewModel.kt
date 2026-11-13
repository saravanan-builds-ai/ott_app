package `in`.sms.tringapps.feature.seeall.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.sms.tringapps.catalog.domain.model.Video
import `in`.sms.tringapps.catalog.domain.usecase.GetPagedPlaylistUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPagedPlaylist: GetPagedPlaylistUseCase,
) : ViewModel() {

    val title: String = savedStateHandle.get<String>(ARG_TITLE).orEmpty()

    val videos: Flow<PagingData<Video>> =
        savedStateHandle.get<String>(ARG_PLAYLIST_ID)
            ?.takeIf { it.isNotBlank() }
            ?.let { getPagedPlaylist(it).cachedIn(viewModelScope) }
            ?: flowOf(PagingData.empty())

    companion object {
        /** Must match the properties of [in.sms.tringapps.feature.seeall.navigation.SeeAllDestination]. */
        const val ARG_PLAYLIST_ID = "playlistId"
        const val ARG_TITLE = "title"
    }
}
