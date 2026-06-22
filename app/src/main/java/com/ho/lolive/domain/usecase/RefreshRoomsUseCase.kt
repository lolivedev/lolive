package com.ho.lolive.domain.usecase

import com.ho.lolive.core.common.AppResult
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.repository.LiveRepository
import javax.inject.Inject

class RefreshRoomsUseCase @Inject constructor(
    private val repository: LiveRepository,
) {
    suspend operator fun invoke(platform: LivePlatform): AppResult<Unit> = repository.refreshRooms(platform)
}
