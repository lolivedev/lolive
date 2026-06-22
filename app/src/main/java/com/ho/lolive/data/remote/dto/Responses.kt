package com.ho.lolive.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlatformListResponse(
    @SerialName("pingtai")
    val platforms: List<PlatformDto> = emptyList(),
)

@Serializable
data class PlatformDto(
    @SerialName("address")
    val address: String,
    @SerialName("xinimg")
    val iconUrl: String,
    @SerialName("Number")
    val onlineCount: String,
    @SerialName("title")
    val title: String,
)

@Serializable
data class LiveRoomListResponse(
    @SerialName("zhubo")
    val anchors: List<AnchorDto> = emptyList(),
)

@Serializable
data class AnchorDto(
    @SerialName("address")
    val streamUrl: String,
    @SerialName("img")
    val coverUrl: String,
    @SerialName("title")
    val title: String,
)
