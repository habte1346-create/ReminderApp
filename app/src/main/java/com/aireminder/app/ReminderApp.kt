package com.aireminder.app

import android.app.Application
import androidx.room.Room
import com.aireminder.app.data.AppDatabase

class ReminderApp : Application() {
    companion object {
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        // We use a new name "remindr-db-v2" to force a clean database start
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "remindr-db-v2"
        ).fallbackToDestructiveMigration().build()
    }
}
