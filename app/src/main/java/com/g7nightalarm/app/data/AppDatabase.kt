package com.g7nightalarm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.g7nightalarm.app.mode.ModeChangeEvent
import com.g7nightalarm.app.mode.ModeDao
import com.g7nightalarm.app.BuildConfig

@Database(
    entities = [GlucoseReading::class, ModeChangeEvent::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
    abstract fun modeDao(): ModeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "g7-night-alarm.db"
                ).fallbackToDestructiveMigration()
                    .apply { if (BuildConfig.DEBUG) allowMainThreadQueries() }
                    .build()
                    .also { instance = it }
            }
    }
}

