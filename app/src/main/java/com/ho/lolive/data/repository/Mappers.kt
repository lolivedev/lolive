package com.ho.lolive.data.repository

import com.ho.lolive.data.local.entity.LiveRoomEntity
import com.ho.lolive.domain.model.LiveRoom
import com.ho.lolive.domain.model.LiveRoomDetail
import com.ho.lolive.domain.model.Quality

fun LiveRoomEntity.toDomain(): LiveRoom = LiveRoom(
    id = id,
    title = title,
    coverUrl = coverUrl,
    platformTitle = platformTitle,
    platformIconUrl = platformIconUrl,
    viewerCount = viewerCount,
)

fun LiveRoomEntity.toDetail(): LiveRoomDetail {
    val sourceUrl = streamUrl.normalizeStreamUrl()
    return LiveRoomDetail(
        id = id,
        title = title,
        streamUrl = sourceUrl,
        qualities = listOf(Quality("Source", sourceUrl)),
        drmLicenseUrl = null,
    )
}

private fun String.normalizeStreamUrl(): String {
    val value = trim()
    if (value.isEmpty()) return value

    return when {
        value.startsWith("//") -> "https:$value"
        else -> value
    }
}
