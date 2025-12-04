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
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "reminder-db"
        ).build()
    }
}
