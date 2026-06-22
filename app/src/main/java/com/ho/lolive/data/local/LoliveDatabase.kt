package com.ho.lolive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ho.lolive.data.local.dao.LiveRoomDao
import com.ho.lolive.data.local.entity.LiveRoomEntity

@Database(
    entities = [LiveRoomEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class LoliveDatabase : RoomDatabase() {
    abstract fun liveRoomDao(): LiveRoomDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE live_rooms ADD COLUMN viewerCount INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_live_rooms_updatedAt ON live_rooms(updatedAt)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_live_rooms_platformTitle_updatedAt ON " +
                        "live_rooms(platformTitle, updatedAt)",
                )
            }
        }
    }
}
