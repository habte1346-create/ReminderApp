package com.aireminder.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.aireminder.app.ui.HomeScreen

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by remember { mutableStateOf(systemDark) }

            MaterialTheme(colorScheme = if(isDark) darkColorScheme() else lightColorScheme()) {
                HomeScreen(
                    dao = ReminderApp.database.reminderDao(),
                    isDark = isDark,
                    onToggleTheme = { isDark = !isDark },
                    context = this
                )
            }
        }
    }
}
