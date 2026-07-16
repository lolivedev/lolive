package com.ho.lolive.presentation.home

import com.ho.lolive.domain.model.AppUpdateInfo
import com.ho.lolive.domain.model.LivePlatform

/** 手动检查更新后的一次性 Toast 类型（由 Activity 映射文案）。 */
enum class UpdateCheckToast {
    ALREADY_LATEST,
    CHECK_FAILED,
}

data class HomeUiState(
    val query: String = "",
    val platforms: List<LivePlatform> = emptyList(),
    val selectedPlatformAddress: String? = null,
    val isLoadingPlatforms: Boolean = false,
    val isRefreshingRooms: Boolean = false,
    val errorMessage: String? = null,
    val networkConnected: Boolean = true,
    val availableUpdate: AppUpdateInfo? = null,
    val updateCheckToast: UpdateCheckToast? = null,
    val isCheckingUpdate: Boolean = false,
)
