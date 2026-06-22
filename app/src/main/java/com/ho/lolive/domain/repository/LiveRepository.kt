package com.ho.lolive.domain.repository

import androidx.paging.PagingData
import com.ho.lolive.core.common.AppResult
import com.ho.lolive.domain.model.AppUpdateInfo
import com.ho.lolive.domain.model.LivePlatform
import com.ho.lolive.domain.model.LiveRoom
import com.ho.lolive.domain.model.LiveRoomDetail
import kotlinx.coroutines.flow.Flow

interface LiveRepository {
    fun observePagedRooms(query: String): Flow<PagingData<LiveRoom>>
    suspend fun getPlatforms(): AppResult<List<LivePlatform>>
    suspend fun refreshRooms(platform: LivePlatform): AppResult<Unit>
    suspend fun getRoomDetail(roomId: String): AppResult<LiveRoomDetail>
    suspend fun getPreviousRoomId(roomId: String): String?
    suspend fun getNextRoomId(roomId: String): String?
    suspend fun getLatestRelease(): AppResult<AppUpdateInfo>
}
