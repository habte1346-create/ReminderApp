package com.aireminder.app.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aireminder.app.alarm.AlarmReceiver
import com.aireminder.app.data.Reminder
import com.aireminder.app.data.ReminderDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(dao: ReminderDao, isDark: Boolean, onToggleTheme: () -> Unit, context: Context) {
    val reminders by dao.getAll().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Reminders") },
                actions = {
                    TextButton(onClick = onToggleTheme) {
                        Text(if (isDark) "Light Mode" else "Dark Mode")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(reminders) { reminder ->
                ReminderItem(reminder) {
                    scope.launch { dao.delete(reminder) }
                }
            }
        }

        if (showDialog) {
            AddDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, msg, minutes ->
                    scope.launch {
                        val time = System.currentTimeMillis() + (minutes * 60 * 1000)
                        val r = Reminder(title = title, message = msg, timestamp = time)
                        val id = dao.insert(r)
                        scheduleAlarm(context, id.toInt(), title, msg, time)
                        showDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun ReminderItem(reminder: Reminder, onDelete: () -> Unit) {
    val date = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminder.timestamp))
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                Text(reminder.message, style = MaterialTheme.typography.bodyMedium)
                Text("Alarm: $date", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Del") }
        }
    }
}

@Composable
fun AddDialog(onDismiss: () -> Unit, onConfirm: (String, String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var mins by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(title, msg, mins.toLongOrNull() ?: 1) }) { Text("Set") }
        },
        title = { Text("New Reminder") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("Message") })
                OutlinedTextField(value = mins, onValueChange = { mins = it }, label = { Text("Minutes from now") })
            }
        }
    )
}

fun scheduleAlarm(context: Context, id: Int, title: String, msg: String, time: Long) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val i = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("TITLE", title)
        putExtra("MESSAGE", msg)
    }
    val pi = PendingIntent.getBroadcast(context, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
    Toast.makeText(context, "Alarm set!", Toast.LENGTH_SHORT).show()
}
