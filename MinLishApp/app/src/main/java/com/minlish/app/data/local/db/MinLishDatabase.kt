package com.minlish.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DashboardCache::class,
        ProgressCache::class,
        LearningPlanCache::class,
        DeckEntity::class,
        CardEntity::class,
        PendingReviewEntity::class,
        ProfileCache::class,
        UserSettingsCache::class,
        NotificationSummaryCache::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MinLishDatabase : RoomDatabase() {

    abstract fun dao(): MinLishDao

    companion object {
        @Volatile
        private var INSTANCE: MinLishDatabase? = null

        fun getInstance(context: Context): MinLishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MinLishDatabase::class.java,
                    "minlish_local_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
