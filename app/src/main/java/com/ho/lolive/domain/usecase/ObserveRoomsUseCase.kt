package com.ho.lolive.domain.usecase

import androidx.paging.PagingData
import com.ho.lolive.domain.model.LiveRoom
import com.ho.lolive.domain.repository.LiveRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveRoomsUseCase @Inject constructor(
    private val repository: LiveRepository,
) {
    operator fun invoke(query: String): Flow<PagingData<LiveRoom>> = repository.observePagedRooms(query)
}
