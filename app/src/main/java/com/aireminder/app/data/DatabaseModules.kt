package com.aireminder.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Enums for repeat logic
enum class RepeatMode { NONE, HOURLY, DAILY, WEEKLY, MONTHLY }

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long,
    val iconId: Int,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val isCompleted: Boolean = false
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY timestamp ASC")
    fun getActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY timestamp DESC")
    fun getRecent(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun clearRecent()
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Int): Reminder?
}

@Database(
    entities = [Reminder::class],
    version = 2,
    exportSchema = false  // 🚀 Fix added here
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}