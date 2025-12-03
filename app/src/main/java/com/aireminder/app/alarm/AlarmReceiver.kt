package com.aireminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: "Reminder"
        val message = intent.getStringExtra("MESSAGE") ?: ""

        // Hand off to WorkManager (Background Worker) to handle Network + Notification
        val request = OneTimeWorkRequestBuilder<AiWorker>()
            .setInputData(workDataOf("TITLE" to title, "MESSAGE" to message))
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
