package com.ho.lolive.domain.usecase

import com.ho.lolive.core.common.AppResult
import com.ho.lolive.domain.model.LiveRoomDetail
import com.ho.lolive.domain.repository.LiveRepository
import javax.inject.Inject

class GetRoomDetailUseCase @Inject constructor(
    private val repository: LiveRepository,
) {
    suspend operator fun invoke(roomId: String): AppResult<LiveRoomDetail> = repository.getRoomDetail(roomId)
}
