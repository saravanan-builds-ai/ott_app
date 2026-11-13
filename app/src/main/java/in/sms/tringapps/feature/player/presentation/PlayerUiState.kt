package `in`.sms.tringapps.feature.player.presentation

import `in`.sms.tringapps.catalog.domain.model.Video

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    /** [startPositionMs] restores playback after process death or a retry. */
    data class Ready(val video: Video, val startPositionMs: Long) : PlayerUiState

    data class Error(val reason: PlayerError) : PlayerUiState
}

enum class PlayerError { NETWORK, NOT_FOUND, NOT_PLAYABLE, PLAYBACK }
