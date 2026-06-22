package com.ho.lolive.data.remote

import com.ho.lolive.data.remote.dto.LiveRoomListResponse
import com.ho.lolive.data.remote.dto.PlatformListResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface LiveApiService {
    @GET
    suspend fun getPlatforms(@Url url: String): PlatformListResponse

    @GET
    suspend fun getPlatformRooms(@Url url: String): LiveRoomListResponse
}
