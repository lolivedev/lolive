package com.ho.lolive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "live_rooms",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["platformTitle", "updatedAt"]),
    ],
)
data class LiveRoomEntity(
    @PrimaryKey val id: String,
    val title: String,
    val coverUrl: String,
    val streamUrl: String,
    val platformTitle: String,
    val platformIconUrl: String,
    val viewerCount: Int,
    val updatedAt: Long,
)
