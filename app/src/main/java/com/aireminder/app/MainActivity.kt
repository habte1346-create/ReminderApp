package com.aireminder.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aireminder.app.alarm.scheduleAlarm
import com.aireminder.app.data.Reminder
import com.aireminder.app.data.RepeatMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val prefs = getSharedPreferences("remindr_prefs", Context.MODE_PRIVATE)
        // Load the saved setting, default to true (Dark Mode)
        val isDark = prefs.getBoolean("dark_mode", true)

        setContent {
            // We use a state here so the UI updates immediately when toggled
            var darkMode by remember { mutableStateOf(isDark) }

            RemindrTheme(darkMode) {
                MainScreen(
                    isDark = darkMode,
                    onToggleTheme = {
                        val newMode = !darkMode
                        darkMode = newMode
                        prefs.edit().putBoolean("dark_mode", newMode).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun RemindrTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF92FE9D),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00C9FF),
            background = Color(0xFFF5F5F5),
            surface = Color.White,
            onSurface = Color.Black
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(isDark: Boolean, onToggleTheme: () -> Unit) {
    val dao = ReminderApp.database.reminderDao()
    val activeTasks by dao.getActive().collectAsState(initial = emptyList())
    val recentTasks by dao.getRecent().collectAsState(initial = emptyList())
    
    // FIX: Capture the context HERE (at the top), not inside the button click
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF2C3E50)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE0EAFC)))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(20.dp))
                Text("Remindr", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                Divider()
                Box(Modifier.padding(16.dp)) {
                    Column {
                        Text("Builder: Habte", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Help & Support:", style = MaterialTheme.typography.labelLarge)
                        Text("Telegram: @habteXYZ", style = MaterialTheme.typography.bodySmall)
                        Text("Phone: +251974524779", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REMINDR", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text("by Habte", fontSize = 10.sp, modifier = Modifier.alpha(0.6f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            Icon(if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, "Theme")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Add, "Add", tint = Color.Black)
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().background(bgBrush).padding(padding)) {
                
                if (activeTasks.isEmpty() && recentTasks.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).alpha(0.3f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.EventNote, null, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No tasks scheduled.", fontSize = 18.sp)
                        Text("Tap + to get started", fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        if (activeTasks.isNotEmpty()) {
                            item { Text("UPCOMING", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(0.5f)) }
                            items(activeTasks) { task ->
                                TaskCard(task, isRecent = false, dao = dao)
                            }
                        }
                        
                        if (recentTasks.isNotEmpty()) {
                            item { 
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top=24.dp)) {
                                    Text("RECENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(0.5f).weight(1f))
                                    TextButton(onClick = { scope.launch { dao.clearRecent() } }) {
                                        Text("Clear")
                                    }
                                }
                            }
                            items(recentTasks) { task ->
                                TaskCard(task, isRecent = true, dao = dao)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, time, icon, repeat ->
                scope.launch {
                    val r = Reminder(title = title, timestamp = time, iconId = icon, repeatMode = repeat)
                    val id = dao.insert(r)
                    // FIX: Use the 'context' variable we captured at the top
                    scheduleAlarm(context, r.copy(id = id.toInt())) 
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun TaskCard(task: Reminder, isRecent: Boolean, dao: com.aireminder.app.data.ReminderDao) {
    val icons = listOf(
        Icons.Rounded.Work, Icons.Rounded.School, Icons.Rounded.LocalHospital, Icons.Rounded.FitnessCenter,
        Icons.Rounded.ShoppingCart, Icons.Rounded.Flight, Icons.Rounded.Home, Icons.Rounded.Restaurant,
        Icons.Rounded.Code, Icons.Rounded.MusicNote, Icons.Rounded.Book, Icons.Rounded.Star
    )
    val scope = rememberCoroutineScope()
    val dateStr = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault()).format(Date(task.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(if(isRecent) 0.6f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icons.getOrElse(task.iconId) { Icons.Rounded.Star }, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                if(task.repeatMode != RepeatMode.NONE) {
                    Text("Repeats: ${task.repeatMode.name}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = { scope.launch { dao.delete(task) } }) {
                Icon(Icons.Rounded.Delete, "Delete", tint = Color.Red.copy(alpha=0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String, Long, Int, RepeatMode) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(0) }
    var repeatMode by remember { mutableStateOf(RepeatMode.NONE) }
    
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timeState = rememberTimePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val icons = listOf(
        Icons.Rounded.Work, Icons.Rounded.School, Icons.Rounded.LocalHospital, Icons.Rounded.FitnessCenter,
        Icons.Rounded.ShoppingCart, Icons.Rounded.Flight, Icons.Rounded.Home, Icons.Rounded.Restaurant,
        Icons.Rounded.Code, Icons.Rounded.MusicNote, Icons.Rounded.Book, Icons.Rounded.Star
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Reminder") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Set Date")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Set Time")
                    }
                }
                
                val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Text("Scheduled: ${fmt.format(Date(selectedDate))}", fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))

                Spacer(Modifier.height(16.dp))
                
                Text("Repeat", fontWeight = FontWeight.Bold)
                ScrollableTabRow(selectedTabIndex = repeatMode.ordinal, edgePadding = 0.dp) {
                    RepeatMode.values().forEach { mode ->
                        Tab(
                            selected = repeatMode == mode,
                            onClick = { repeatMode = mode },
                            text = { Text(mode.name, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Icon", fontWeight = FontWeight.Bold)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6), 
                    modifier = Modifier.height(80.dp)
                ) {
                    itemsIndexed(icons) { index, icon ->
                        IconButton(onClick = { selectedIcon = index }) {
                            Icon(
                                icon, 
                                null, 
                                tint = if(selectedIcon == index) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(title, selectedDate, selectedIcon, repeatMode)
            }) { Text("Save") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { 
                        val c = Calendar.getInstance().apply { timeInMillis = it }
                        val current = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        current.set(Calendar.YEAR, c.get(Calendar.YEAR))
                        current.set(Calendar.MONTH, c.get(Calendar.MONTH))
                        current.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH))
                        selectedDate = current.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = dateState) }
    }
    
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    c.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    c.set(Calendar.MINUTE, timeState.minute)
                    selectedDate = c.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
