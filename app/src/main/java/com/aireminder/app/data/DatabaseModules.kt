package com.aireminder.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY timestamp ASC")
    fun getAll(): Flow<List<Reminder>>

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Delete
    suspend fun delete(reminder: Reminder)
}

@Database(entities = [Reminder::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
