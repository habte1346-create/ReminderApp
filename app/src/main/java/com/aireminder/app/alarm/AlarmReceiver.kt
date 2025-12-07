package com.aireminder.app.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aireminder.app.ReminderApp
import com.aireminder.app.data.Reminder
import com.aireminder.app.data.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("ID", -1)
        if (id == -1) return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val dao = ReminderApp.database.reminderDao()
            val reminder = dao.getById(id) ?: return@launch

            // 1. Show Notification
            showNotification(context, reminder)

            // 2. Handle Repeat or Completion
            if (reminder.repeatMode != RepeatMode.NONE) {
                // Calculate next time
                val nextTime = calculateNextTime(reminder.timestamp, reminder.repeatMode)
                val newReminder = reminder.copy(timestamp = nextTime)
                dao.update(newReminder)
                scheduleAlarm(context, newReminder)
            } else {
                // Mark as completed/recent
                dao.update(reminder.copy(isCompleted = true))
            }
        }
    }

    private fun calculateNextTime(current: Long, mode: RepeatMode): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = current
        val now = System.currentTimeMillis()
        
        // Advance time until it is in the future
        while (cal.timeInMillis <= now) {
            when (mode) {
                RepeatMode.HOURLY -> cal.add(Calendar.HOUR_OF_DAY, 1)
                RepeatMode.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RepeatMode.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatMode.MONTHLY -> cal.add(Calendar.MONTH, 1)
                else -> cal.add(Calendar.MINUTE, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun showNotification(context: Context, reminder: Reminder) {
        val channelId = "remindr_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Remindr Tasks", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(reminder.title)
            .setContentText("Reminder for now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(reminder.id, notification)
    }
}

fun scheduleAlarm(context: Context, reminder: Reminder) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("ID", reminder.id)
    }
    val pi = PendingIntent.getBroadcast(
        context, reminder.id, intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timestamp, pi)
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
