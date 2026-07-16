package com.ho.lolive.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ho.lolive.data.local.entity.LiveRoomEntity

@Dao
interface LiveRoomDao {
    @Query(
        """
        SELECT * FROM live_rooms
        WHERE title LIKE '%' || :query || '%' ESCAPE '\'
           OR platformTitle LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY updatedAt DESC
        """,
    )
    fun pagingSource(query: String): PagingSource<Int, LiveRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<LiveRoomEntity>)

    @Query("DELETE FROM live_rooms WHERE platformTitle = :platformTitle")
    suspend fun clearByPlatform(platformTitle: String)

    @Transaction
    suspend fun replacePlatformRooms(platformTitle: String, rooms: List<LiveRoomEntity>) {
        clearByPlatform(platformTitle)
        insertAll(rooms)
    }

    @Query("SELECT * FROM live_rooms WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): LiveRoomEntity?

    @Query(
        """
        SELECT id FROM live_rooms
        WHERE platformTitle = :platformTitle AND updatedAt > :currentUpdatedAt
        ORDER BY updatedAt ASC, title COLLATE NOCASE ASC
        LIMIT 1
        """,
    )
    suspend fun findPreviousRoomId(platformTitle: String, currentUpdatedAt: Long): String?

    @Query(
        """
        SELECT id FROM live_rooms
        WHERE platformTitle = :platformTitle AND updatedAt < :currentUpdatedAt
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        LIMIT 1
        """,
    )
    suspend fun findNextRoomId(platformTitle: String, currentUpdatedAt: Long): String?

    @Query(
        """
        SELECT id FROM live_rooms
        WHERE platformTitle = :platformTitle
        ORDER BY updatedAt ASC, title COLLATE NOCASE ASC
        LIMIT 1
        """,
    )
    suspend fun findOldestRoomId(platformTitle: String): String?

    @Query(
        """
        SELECT id FROM live_rooms
        WHERE platformTitle = :platformTitle
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        LIMIT 1
        """,
    )
    suspend fun findNewestRoomId(platformTitle: String): String?
}
