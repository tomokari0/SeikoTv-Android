package com.example.data.datasource.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WatchProgressEntity::class,
        DownloadEntity::class,
        MyListEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SeikoDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun myListDao(): MyListDao

    companion object {
        @Volatile
        private var INSTANCE: SeikoDatabase? = null

        fun getInstance(context: Context): SeikoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SeikoDatabase::class.java,
                    "seikotv_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
