package com.ho.lolive.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ho.lolive.BuildConfig
import com.ho.lolive.core.common.AppResult
import com.ho.lolive.core.common.Logger
import com.ho.lolive.core.network.ConnectivityObserver
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.usecase.CheckAppUpdateUseCase
import com.ho.lolive.domain.usecase.GetPlatformsUseCase
import com.ho.lolive.domain.usecase.ObserveRoomsUseCase
import com.ho.lolive.domain.usecase.RefreshRoomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeRoomsUseCase: ObserveRoomsUseCase,
    private val getPlatformsUseCase: GetPlatformsUseCase,
    private val refreshRoomsUseCase: RefreshRoomsUseCase,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val queryState = MutableStateFlow("")
    private var platformsLoadJob: Job? = null
    private var refreshJob: Job? = null

    val pagedRooms = queryState
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            observeRoomsUseCase(query)
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            connectivityObserver.observe().collect { connected ->
                val previousConnected = _uiState.value.networkConnected
                _uiState.update { it.copy(networkConnected = connected) }
                val reconnected = !previousConnected && connected
                if (reconnected && _uiState.value.platforms.isEmpty()) {
                    loadPlatforms()
                }
            }
        }
        loadPlatforms()
        checkForUpdate()
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryState.value = newQuery.trim()
    }

    fun onPlatformSelected(address: String) {
        val state = _uiState.value
        if (state.selectedPlatformAddress == address) return

        val selected = state.platforms.firstOrNull { it.address == address } ?: return
        _uiState.update { it.copy(selectedPlatformAddress = address, errorMessage = null) }
        loadRoomsForPlatform(selected)
    }

    fun refresh() {
        val state = _uiState.value
        val selected = state.platforms.firstOrNull { it.address == state.selectedPlatformAddress }
        if (selected != null) {
            loadRoomsForPlatform(selected)
        } else {
            loadPlatforms()
        }
    }

    fun refreshPlatforms() {
        loadPlatforms()
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(availableUpdate = null) }
    }

    private fun loadPlatforms() {
        if (platformsLoadJob?.isActive == true) return

        platformsLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlatforms = true, errorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    getPlatformsUseCase()
                }
                when (result) {
                    is AppResult.Success -> {
                        val onlinePlatforms = result.data.filter { it.onlineCount > 0 }
                        val platforms = onlinePlatforms.ifEmpty { result.data }
                        val preferredAddress = _uiState.value.selectedPlatformAddress
                        val selected = platforms.firstOrNull { it.address == preferredAddress } ?: platforms.firstOrNull()

                        _uiState.update {
                            it.copy(
                                platforms = platforms,
                                selectedPlatformAddress = selected?.address,
                                isLoadingPlatforms = false,
                                errorMessage = null,
                            )
                        }

                        if (selected != null) {
                            loadRoomsForPlatform(selected)
                        }
                    }

                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingPlatforms = false,
                                errorMessage = result.message ?: "unknown error",
                            )
                        }
                    }

                    AppResult.Loading -> Unit
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                _uiState.update { state ->
                    if (state.isLoadingPlatforms) state.copy(isLoadingPlatforms = false) else state
                }
            }
        }
    }

    private fun loadRoomsForPlatform(platform: LivePlatform) {
        // Cancel any in-flight refresh (e.g. for a different platform) so switching platforms is
        // never silently dropped by a stale in-progress request.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRooms = true, errorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    refreshRoomsUseCase(platform)
                }
                when (result) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> {
                        _uiState.update {
                            it.copy(errorMessage = result.message ?: "unknown error")
                        }
                    }
                    AppResult.Loading -> Unit
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                // Only clear the refreshing flag when this launch is still the active refresh for
                // the currently selected platform; otherwise a superseded launch would clobber the
                // spinner of the newer one.
                _uiState.update { state ->
                    if (state.selectedPlatformAddress == platform.address) {
                        state.copy(isRefreshingRooms = false)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                checkAppUpdateUseCase(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                )
            }

            when (result) {
                is AppResult.Success -> {
                    val update = result.data ?: return@launch
                    _uiState.update { it.copy(availableUpdate = update) }
                }

                is AppResult.Error -> {
                    Logger.e("checkForUpdate failed", result.throwable)
                }

                AppResult.Loading -> Unit
            }
        }
    }
}
