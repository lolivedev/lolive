package com.ho.lolive.presentation.home

import com.ho.lolive.domain.model.AppUpdateInfo
import com.ho.lolive.domain.model.LivePlatform

data class HomeUiState(
    val query: String = "",
    val platforms: List<LivePlatform> = emptyList(),
    val selectedPlatformAddress: String? = null,
    val isLoadingPlatforms: Boolean = false,
    val isRefreshingRooms: Boolean = false,
    val errorMessage: String? = null,
    val networkConnected: Boolean = true,
    val availableUpdate: AppUpdateInfo? = null,
)
