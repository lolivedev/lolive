package com.ho.lolive.presentation.detail

import com.ho.lolive.domain.model.LiveRoomDetail

enum class FullScreenMode {
    NONE,
    LANDSCAPE,
    PORTRAIT,
}

data class DetailUiState(
    val loading: Boolean = true,
    val room: LiveRoomDetail? = null,
    val currentRoomId: String? = null,
    val previousRoomId: String? = null,
    val nextRoomId: String? = null,
    val errorMessage: String? = null,
    val fullScreenMode: FullScreenMode = FullScreenMode.NONE,
    val networkConnected: Boolean = true,
    val retryToken: Int = 0,
    val playerErrorMessage: String? = null,
    val maxRetryReached: Boolean = false,
)
