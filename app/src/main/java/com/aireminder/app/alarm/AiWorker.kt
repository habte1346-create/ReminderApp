package com.aireminder.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aireminder.app.R
import com.aireminder.app.ReminderApp
import com.aireminder.app.network.*

class AiWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("TITLE") ?: "Reminder"
        val originalMsg = inputData.getString("MESSAGE") ?: ""
        var finalText = originalMsg

        try {
            // 1. Get Key from Railway
            val keyApi = NetworkModule.getKeyApi(ReminderApp.RAILWAY_URL)
            val apiKey = keyApi.getGroqKey().key

            // 2. Ask Groq for Summary
            val groqApi = NetworkModule.getGroqApi()
            val response = groqApi.summarize(
                token = "Bearer $apiKey",
                request = GroqRequest(
                    model = "llama3-8b-8192",
                    messages = listOf(
                        Message("system", "Summarize this in 10 words or less."),
                        Message("user", originalMsg)
                    )
                )
            )
            finalText = "AI: " + response.choices.first().message.content
        } catch (e: Exception) {
            e.printStackTrace()
            // If offline, just show original message
        }

        sendNotification(title, finalText)
        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "ai_reminders"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
