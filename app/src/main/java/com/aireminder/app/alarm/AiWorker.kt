package com.aireminder.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aireminder.app.R
import com.aireminder.app.network.*

class AiWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("TITLE") ?: "Reminder"
        val originalMsg = inputData.getString("MESSAGE") ?: ""
        var finalText = originalMsg

        try {
            // OPTION B: Hardcoded Key (Emergency Fix)
            // We are skipping the server check and putting the key directly here.
            val apiKey = "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"

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
            // Update the message with the AI summary
            finalText = "AI: " + response.choices.first().message.content
        } catch (e: Exception) {
            e.printStackTrace()
            // If the internet is off or API fails, we just show the original message
            finalText = originalMsg
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Allows long text to show
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
