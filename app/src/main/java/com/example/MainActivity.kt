package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "AXS Controller",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            navigationIcon = {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.testTag("menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.testTag("profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Account",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AccessibilityDashboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class SystemAction(
    val label: String,
    val command: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val testTag: String
)

@Composable
fun AccessibilityDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isEnabled by remember { mutableStateOf(false) }

    // Check service status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isEnabled = isAccessibilityServiceEnabled(context, MyAccessibilityService::class.java)
            delay(1000)
        }
    }

    val logs by CommandLogManager.logs.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    // On-device click simulation state
    var clickText by remember { mutableStateOf("") }
    var useDelay by remember { mutableStateOf(false) }
    var countdownTimer by remember { mutableStateOf(0) }
    var isTriggering by remember { mutableStateOf(false) }

    // Pulsing animation for active indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Supported System/Keyboard actions
    val systemActions = remember {
        listOf(
            SystemAction("Back", "back", Icons.Default.ArrowBack, "Simulate Back button press", "trigger_back_btn"),
            SystemAction("Home", "home", Icons.Default.Home, "Simulate Home button press", "trigger_home_btn"),
            SystemAction("Recents", "recents", Icons.Default.List, "Show recent apps", "trigger_recents_btn"),
            SystemAction("Notifications", "notifications", Icons.Default.Notifications, "Open notifications panel", "trigger_notifs_btn"),
            SystemAction("Quick Settings", "quick_settings", Icons.Default.Settings, "Open quick settings panel", "trigger_quick_settings_btn"),
            SystemAction("Power Menu", "power_dialog", Icons.Default.Warning, "Show standard power options dialog", "trigger_power_dialog_btn"),
            SystemAction("Lock Screen", "lock_screen", Icons.Default.Lock, "Lock the screen immediately", "trigger_lock_screen_btn"),
            SystemAction("Split Screen", "split_screen", Icons.Default.Star, "Toggle split screen multi-window mode", "trigger_split_screen_btn")
        )
    }

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Service Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_status_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ACCESSIBILITY SERVICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (isEnabled) "ACTIVE" else "INACTIVE",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Rounded icon container
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEnabled) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                                contentDescription = "Status Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(if (isEnabled) pulseAlpha else 1.0f)
                                .background(
                                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isEnabled) "Running & waiting for on-device actions" else "Tap below to enable Accessibility Service",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (!isEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enable_service_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Enable Service in Settings",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Delay countdown alert if running
        if (isTriggering && countdownTimer > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                        Column {
                            Text(
                                text = "Switching Apps Now!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Simulating click on '$clickText' in $countdownTimer seconds...",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // On-Device Click Simulator (ADB-Free automation)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ON-DEVICE CLICK SIMULATOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Type text of any button or element on screen to click it.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = clickText,
                        onValueChange = { clickText = it },
                        placeholder = { Text("e.g. Settings, Submit, Cancel, etc.") },
                        label = { Text("Target Element Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { useDelay = !useDelay },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = useDelay,
                            onCheckedChange = { useDelay = it },
                            modifier = Modifier.testTag("delay_checkbox")
                        )
                        Column {
                            Text(
                                text = "Add 3-Second Countdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gives you time to switch to another app screen before click triggers.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (clickText.trim().isNotEmpty()) {
                                if (useDelay) {
                                    isTriggering = true
                                    coroutineScope.launch {
                                        countdownTimer = 3
                                        while (countdownTimer > 0) {
                                            delay(1000)
                                            countdownTimer--
                                        }
                                        sendTestBroadcast(context, clickText)
                                        isTriggering = false
                                        clickText = ""
                                    }
                                } else {
                                    sendTestBroadcast(context, clickText)
                                    clickText = ""
                                }
                            }
                        },
                        enabled = clickText.trim().isNotEmpty() && !isTriggering,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("trigger_click_action_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTriggering) "Waiting..." else "Execute Click Action",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // On-Device System Actions Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ON-DEVICE NAVIGATION & SYSTEM CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Grid layout with 2 columns
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val chunks = systemActions.chunked(2)
                    for (row in chunks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (action in row) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = isEnabled) {
                                            sendTestBroadcast(context, action.command)
                                        }
                                        .testTag(action.testTag),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = if (isEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = action.icon,
                                                contentDescription = action.label,
                                                tint = if (isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = action.label,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = action.description,
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Actions Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTION LOG HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            CommandLogManager.logs.value = emptyList()
                        },
                        modifier = Modifier.testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                "No actions recorded yet.\nActivate controls above or trigger dynamic clicks.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(logs) { log ->
                BoldLogItem(log)
            }
        }

        // ADB Copy Command Dropdown Reference (Collapsible/Secondary)
        item {
            var expandedAdb by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.clickable { expandedAdb = !expandedAdb }.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                "Developer ADB Command Reference",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (expandedAdb) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (expandedAdb) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If you want to trigger clicks or navigation externally from a computer instead of the UI, use this standard ADB broadcast command template:",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        val adbCmd = "adb shell am broadcast -a com.research.AXS_COMMAND --es target_button \"back\""
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = adbCmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(adbCmd)) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("copy_adb_cmd")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy command",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoldLogItem(log: CommandLog) {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val formattedTime = sdf.format(Date(log.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (log.key.trim().lowercase()) {
                    "back", "@back" -> Icons.Default.ArrowBack
                    "home", "@home" -> Icons.Default.Home
                    "recents", "@recents" -> Icons.Default.List
                    "notifications", "@notifications" -> Icons.Default.Notifications
                    "quick_settings", "@quick_settings" -> Icons.Default.Settings
                    "lock_screen", "@lock_screen" -> Icons.Default.Lock
                    "service_connected" -> Icons.Default.CheckCircle
                    "service_disconnected" -> Icons.Default.Cancel
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = when (log.key.trim().lowercase()) {
                    "back", "@back", "home", "@home", "recents", "@recents", "notifications", "@notifications" -> MaterialTheme.colorScheme.primary
                    "service_connected" -> Color(0xFF2E7D32)
                    "service_disconnected" -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.key.uppercase(Locale.getDefault()),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.status,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formattedTime,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            thickness = 1.dp
        )
    }
}

private fun sendTestBroadcast(context: Context, key: String) {
    val intent = Intent("com.research.AXS_COMMAND")
    intent.putExtra("target_button", key)
    intent.putExtra("key", key) // support backward compatibility with both extras
    context.sendBroadcast(intent)
}

private fun isAccessibilityServiceEnabled(
    context: Context,
    serviceClass: Class<out AccessibilityService>
): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)
    for (enabledService in enabledServices) {
        val serviceInfo = enabledService.resolveInfo.serviceInfo
        if (serviceInfo.packageName == context.packageName && serviceInfo.name == serviceClass.name) {
            return true
        }
    }
    return false
}
