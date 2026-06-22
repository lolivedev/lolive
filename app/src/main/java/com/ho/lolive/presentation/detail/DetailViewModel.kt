package com.ho.lolive.presentation.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ho.lolive.core.common.AppResult
import com.ho.lolive.core.network.ConnectivityObserver
import com.ho.lolive.domain.usecase.GetAdjacentRoomUseCase
import com.ho.lolive.domain.usecase.GetRoomDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoomDetailUseCase: GetRoomDetailUseCase,
    private val getAdjacentRoomUseCase: GetAdjacentRoomUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private var currentRoomId = Uri.decode(checkNotNull(savedStateHandle.get<String>("roomId")))

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var retryCount = 0

    companion object {
        private const val MAX_RETRY_COUNT = 5
    }

    init {
        loadRoom(currentRoomId)
        viewModelScope.launch {
            connectivityObserver.observe().collect { connected ->
                _uiState.update { it.copy(networkConnected = connected) }
                if (connected) triggerRetry()
            }
        }
    }

    fun setLandscapeFullscreen() {
        _uiState.update { it.copy(fullScreenMode = FullScreenMode.LANDSCAPE) }
    }

    fun setPortraitFullscreen() {
        _uiState.update { it.copy(fullScreenMode = FullScreenMode.PORTRAIT) }
    }

    fun exitFullscreen() {
        _uiState.update { it.copy(fullScreenMode = FullScreenMode.NONE) }
    }

    fun manualRetry() {
        triggerRetry()
    }

    fun playPrevious() {
        val prevId = _uiState.value.previousRoomId ?: return
        loadRoom(prevId)
    }

    fun playNext() {
        val nextId = _uiState.value.nextRoomId ?: return
        loadRoom(nextId)
    }

    fun onPlayerError(message: String) {
        if (retryCount >= MAX_RETRY_COUNT) {
            _uiState.update { it.copy(playerErrorMessage = message, maxRetryReached = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(playerErrorMessage = message) }
            retryCount += 1
            val backoff = (1000L * retryCount.coerceAtMost(5))
            delay(backoff)
            triggerRetry()
        }
    }

    fun dismissPlayerError() {
        _uiState.update { it.copy(playerErrorMessage = null, maxRetryReached = false) }
    }

    private fun loadRoom(roomId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    currentRoomId = roomId,
                    room = null,
                    previousRoomId = null,
                    nextRoomId = null,
                    errorMessage = null,
                    playerErrorMessage = null,
                    maxRetryReached = false,
                )
            }

            when (val result = getRoomDetailUseCase(roomId)) {
                is AppResult.Success -> {
                    retryCount = 0
                    currentRoomId = roomId
                    val previousId = getAdjacentRoomUseCase.previous(roomId)
                    val nextId = getAdjacentRoomUseCase.next(roomId)

                    _uiState.update {
                        it.copy(
                            loading = false,
                            room = result.data,
                            previousRoomId = previousId,
                            nextRoomId = nextId,
                            errorMessage = null,
                            playerErrorMessage = null,
                            maxRetryReached = false,
                        )
                    }
                    triggerRetry()
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            room = null,
                            previousRoomId = null,
                            nextRoomId = null,
                            errorMessage = result.message ?: "load detail failed",
                        )
                    }
                }

                AppResult.Loading -> Unit
            }
        }
    }

    private fun triggerRetry() {
        _uiState.update { it.copy(retryToken = it.retryToken + 1) }
    }
}
