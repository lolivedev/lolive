package com.ho.lolive.domain.usecase

import com.ho.lolive.domain.repository.LiveRepository
import javax.inject.Inject

class GetAdjacentRoomUseCase @Inject constructor(
    private val repository: LiveRepository,
) {
    suspend fun previous(roomId: String): String? = repository.getPreviousRoomId(roomId)
    suspend fun next(roomId: String): String? = repository.getNextRoomId(roomId)
}
