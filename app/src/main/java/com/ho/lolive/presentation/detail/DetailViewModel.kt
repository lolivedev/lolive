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
import kotlinx.coroutines.Job
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
    private var loadRoomJob: Job? = null
    private var retryJob: Job? = null
    private var hadNetworkLoss = false
    private var pendingPlaybackRetry = false

    companion object {
        private const val MAX_RETRY_COUNT = 5
    }

    init {
        loadRoom(currentRoomId)
        viewModelScope.launch {
            connectivityObserver.observe().collect { connected ->
                _uiState.update { it.copy(networkConnected = connected) }
                if (!connected) {
                    hadNetworkLoss = true
                    return@collect
                }
                // 只在「曾断网」或「播放错误待重试」时恢复，避免首次连通/抖动打断正常播放。
                if (hadNetworkLoss || pendingPlaybackRetry) {
                    hadNetworkLoss = false
                    retryCount = 0
                    scheduleRetry(immediate = true)
                }
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
        retryCount = 0
        pendingPlaybackRetry = true
        scheduleRetry(immediate = true)
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
            pendingPlaybackRetry = true
            _uiState.update { it.copy(playerErrorMessage = message, maxRetryReached = true) }
            return
        }
        pendingPlaybackRetry = true
        _uiState.update { it.copy(playerErrorMessage = message, maxRetryReached = false) }
        retryCount += 1
        val backoff = 1000L * retryCount.coerceAtMost(5)
        scheduleRetry(immediate = false, backoffMs = backoff)
    }

    fun dismissPlayerError() {
        _uiState.update { it.copy(playerErrorMessage = null, maxRetryReached = false) }
    }

    private fun loadRoom(roomId: String) {
        // Cancel any in-flight load so a late-arriving result cannot overwrite the newest selection.
        loadRoomJob?.cancel()
        retryJob?.cancel()
        retryCount = 0
        pendingPlaybackRetry = false
        hadNetworkLoss = false
        loadRoomJob = viewModelScope.launch {
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
                    pendingPlaybackRetry = false
                    currentRoomId = roomId
                    val previousId = getAdjacentRoomUseCase.previous(roomId)
                    val nextId = getAdjacentRoomUseCase.next(roomId)

                    // 同一次状态更新里递增 retryToken，避免「房间变更 + 额外 token」触发两次 prepare。
                    _uiState.update {
                        it.copy(
                            loading = false,
                            room = result.data,
                            previousRoomId = previousId,
                            nextRoomId = nextId,
                            errorMessage = null,
                            playerErrorMessage = null,
                            maxRetryReached = false,
                            retryToken = it.retryToken + 1,
                        )
                    }
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

    private fun scheduleRetry(immediate: Boolean, backoffMs: Long = 0L) {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            if (!immediate && backoffMs > 0L) {
                delay(backoffMs)
            }
            if (!_uiState.value.networkConnected && !immediate) {
                // 断网时不空转 bump token，等网络恢复分支再触发。
                return@launch
            }
            pendingPlaybackRetry = false
            triggerRetry()
        }
    }

    private fun triggerRetry() {
        _uiState.update { it.copy(retryToken = it.retryToken + 1) }
    }
}
