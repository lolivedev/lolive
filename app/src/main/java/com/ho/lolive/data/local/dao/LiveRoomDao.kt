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
        WHERE title LIKE '%' || :query || '%' OR platformTitle LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """,
    )
    fun pagingSource(query: String): PagingSource<Int, LiveRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<LiveRoomEntity>)

    @Query("DELETE FROM live_rooms")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(rooms: List<LiveRoomEntity>) {
        clearAll()
        insertAll(rooms)
    }

    @Query("SELECT * FROM live_rooms WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): LiveRoomEntity?

    @Query(
        """
        SELECT id FROM live_rooms
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        """,
    )
    suspend fun orderedRoomIds(): List<String>

    @Query(
        """
        SELECT id FROM live_rooms
        WHERE platformTitle = :platformTitle
        ORDER BY updatedAt DESC, title COLLATE NOCASE ASC
        """,
    )
    suspend fun orderedRoomIdsByPlatform(platformTitle: String): List<String>
}
