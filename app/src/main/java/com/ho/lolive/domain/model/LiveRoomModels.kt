package com.ho.lolive.domain.model

data class LivePlatform(
    val title: String,
    val address: String,
    val iconUrl: String,
    val onlineCount: Int,
)

data class LiveRoom(
    val id: String,
    val title: String,
    val coverUrl: String,
    val platformTitle: String,
    val platformIconUrl: String,
    val viewerCount: Int,
)

data class LiveRoomDetail(
    val id: String,
    val title: String,
    val streamUrl: String,
    val qualities: List<Quality>,
    val drmLicenseUrl: String?,
)

data class Quality(
    val label: String,
    val url: String,
)
