package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusSession
import com.example.ui.FocusViewModel
import com.example.ui.TimerMode
import com.example.ui.TimerState
import java.text.SimpleDateFormat
import java.util.*

// --- Immersive Theme Colors ---
val FocusBg = Color(0xFF0A1210)
val FocusCardBg = Color(0xFF161D1B)
val FocusPillBg = Color(0xFF1C2624)
val FocusBorderColor = Color(0xFF2D3533)
val FocusAccentColor = Color(0xFFA3D1C6)
val FocusTextPrimaryColor = Color(0xFFE1E3E1)
val FocusTextSecondaryColor = Color(0xFF89938F)

@Composable
fun FocusHomeScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    // Determine active layout tab: 0 = Focus, 1 = Stats, 2 = Settings
    var activeTab by remember { mutableStateOf(0) }

    // Observers
    val timerVal by viewModel.timerValue.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val completedMin by viewModel.dailyCompletedMinutes.collectAsState()
    val targetMin by viewModel.dailyGoalMinutes.collectAsState()
    val streakCount by viewModel.currentStreak.collectAsState()
    val efficiency by viewModel.efficiencyPercent.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FocusBg,
        bottomBar = {
            BottomNavBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when (activeTab) {
                0 -> FocusTabContent(
                    viewModel = viewModel,
                    timerVal = timerVal,
                    timerMode = timerMode,
                    timerState = timerState,
                    completedMin = completedMin,
                    targetMin = targetMin,
                    streakCount = streakCount,
                    efficiency = efficiency,
                    currentQuote = currentQuote
                )
                1 -> StatsTabContent(viewModel = viewModel)
                2 -> SettingsTabContent(viewModel = viewModel)
            }
        }
    }
}

// --- TAB CONTENTS ---

@Composable
fun FocusTabContent(
    viewModel: FocusViewModel,
    timerVal: Long,
    timerMode: TimerMode,
    timerState: TimerState,
    completedMin: Long,
    targetMin: Int,
    streakCount: Int,
    efficiency: Int,
    currentQuote: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header Row
        HeaderSec(streakCount = streakCount)

        // 2. Goal Progress Indicator Card
        Spacer(modifier = Modifier.height(8.dp))
        GoalProgressCard(
            completedMinutes = completedMin,
            targetMinutes = targetMin,
            efficiencyPercent = efficiency
        )

        // 3. Immersive Timer Dial Section
        Spacer(modifier = Modifier.height(16.dp))
        TimerDialSection(
            timerVal = timerVal,
            timerMode = timerMode,
            timerState = timerState,
            viewModel = viewModel
        )

        // 4. Playback / Controls Area
        Spacer(modifier = Modifier.height(16.dp))
        QuoteAndSoundArea(
            currentQuote = currentQuote,
            viewModel = viewModel
        )
    }
}

// --- COMPONENTS ---

@Composable
fun HeaderSec(streakCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SESSION",
                fontSize = 10.sp,
                color = FocusTextSecondaryColor,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Deep Focus",
                fontSize = 22.sp,
                color = FocusTextPrimaryColor,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
        }

        // Streak Count Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(FocusPillBg)
                .border(1.dp, FocusBorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("streak_badge"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "🔥", fontSize = 16.sp)
            Text(
                text = "$streakCount Days",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FocusTextPrimaryColor
            )
        }
    }
}

@Composable
fun GoalProgressCard(
    completedMinutes: Long,
    targetMinutes: Int,
    efficiencyPercent: Int
) {
    val completedHours = completedMinutes.toFloat() / 60f
    val targetHours = targetMinutes.toFloat() / 60f
    val progress = if (targetMinutes > 0) {
        (completedMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(FocusCardBg)
            .border(1.dp, FocusBorderColor, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        // Ambient background glowing blob inside card
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(100.dp)
                .offset(x = 30.dp, y = (-30).dp)
                .blur(40.dp)
                .background(FocusAccentColor.copy(alpha = 0.08f), CircleShape)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "DAILY GOAL",
                        fontSize = 10.sp,
                        color = FocusTextSecondaryColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f / %.1f Hours", completedHours, targetHours),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusTextPrimaryColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EFFICIENCY",
                        fontSize = 10.sp,
                        color = FocusTextSecondaryColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$efficiencyPercent%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusAccentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Glowing Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FocusBorderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    FocusAccentColor.copy(alpha = 0.8f),
                                    FocusAccentColor
                                )
                            )
                        )
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(4.dp),
                            clip = false,
                            ambientColor = FocusAccentColor,
                            spotColor = FocusAccentColor
                        )
                )
            }
        }
    }
}

@Composable
fun TimerDialSection(
    timerVal: Long,
    timerMode: TimerMode,
    timerState: TimerState,
    viewModel: FocusViewModel
) {
    val m = timerVal / 60
    val s = timerVal % 60
    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", m, s)

    // Calculate dynamic arc progress
    val targetValSec = when (timerMode) {
        TimerMode.POMODORO -> viewModel.pomodoroDuration.value * 60
        TimerMode.SHORT_BREAK -> viewModel.shortBreakDuration.value * 60
        TimerMode.LONG_BREAK -> viewModel.longBreakDuration.value * 60
    }
    val elapsedFraction = if (targetValSec > 0) {
        (timerVal.toFloat() / targetValSec).coerceIn(0f, 1f)
    } else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Quick Mode Switcher Row atop the dial
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(FocusCardBg)
                .border(1.dp, FocusBorderColor, RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TimerMode.values().forEach { mode ->
                val isActive = timerMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) FocusPillBg else Color.Transparent)
                        .border(
                            1.dp,
                            if (isActive) FocusBorderColor else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.resetTimer(mode) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = when(mode) {
                            TimerMode.POMODORO -> "Pomodoro"
                            TimerMode.SHORT_BREAK -> "Short Break"
                            TimerMode.LONG_BREAK -> "Long Break"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) FocusAccentColor else FocusTextSecondaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large circular timer rendering
        Box(
            modifier = Modifier
                .size(240.dp)
                .testTag("timer_dial"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 7.dp.toPx()
                // Empty Track circle
                drawCircle(
                    color = FocusPillBg,
                    radius = size.width / 2 - strokeWidth,
                    style = Stroke(width = strokeWidth)
                )
                // Active countdown arc
                drawArc(
                    color = FocusAccentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * elapsedFraction,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth, strokeWidth),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth * 2,
                        size.height - strokeWidth * 2
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeStr,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace, // prevent digit jump jitter!
                    color = FocusTextPrimaryColor,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = timerMode.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = FocusTextSecondaryColor,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Stop & Start / Pause Control Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STOP/RESET
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(FocusPillBg)
                    .border(1.dp, FocusBorderColor, CircleShape)
                    .clickable { viewModel.stopTimer() }
                    .testTag("stop_button"),
                contentAlignment = Alignment.Center
            ) {
                // A solid square block for reset/stop
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FocusTextPrimaryColor)
                )
            }

            // PRIMARY START/PAUSE
            val isRunning = timerState == TimerState.RUNNING
            Button(
                onClick = {
                    if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusAccentColor,
                    contentColor = FocusBg
                ),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .height(56.dp)
                    .width(140.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(32.dp),
                        clip = false,
                        ambientColor = FocusAccentColor.copy(alpha = 0.4f),
                        spotColor = FocusAccentColor.copy(alpha = 0.4f)
                    )
                    .testTag("start_button")
            ) {
                Text(
                    text = if (isRunning) "PAUSE" else "START",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // SKIP
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(FocusPillBg)
                    .border(1.dp, FocusBorderColor, CircleShape)
                    .clickable { viewModel.skipTimer() }
                    .testTag("skip_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏭",
                    color = FocusTextPrimaryColor,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun QuoteAndSoundArea(
    currentQuote: String,
    viewModel: FocusViewModel
) {
    val soundPlaying by viewModel.isAmbientPlaying.collectAsState()
    val activeSound by viewModel.selectedAmbientSound.collectAsState()
    val soundVolume by viewModel.ambientVolume.collectAsState()
    val soundOptions = viewModel.ambientSounds

    var expandSoundDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Curated Quote text box with manual trigger to cycle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.cycleQuote() }
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = currentQuote,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = FocusTextSecondaryColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. Beautiful Ambient Audio Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FocusCardBg)
                .border(1.dp, FocusBorderColor, RoundedCornerShape(20.dp))
                .clickable { expandSoundDialog = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusPillBg)
                        .border(1.dp, FocusBorderColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🍃", fontSize = 16.sp)
                }

                Column {
                    Text(
                        text = "AMBIENT AUDIO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusAccentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = activeSound,
                        fontSize = 13.sp,
                        color = FocusTextPrimaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Play/Pause Action Audio button
            IconButton(
                onClick = { viewModel.toggleAmbientAudio() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FocusPillBg)
                    .border(1.dp, FocusBorderColor, CircleShape)
                    .size(36.dp)
                    .testTag("audio_toggle")
            ) {
                Text(
                    text = if (soundPlaying) "🔊" else "🔈",
                    fontSize = 16.sp,
                    color = if (soundPlaying) FocusAccentColor else FocusTextSecondaryColor
                )
            }
        }
    }

    // Modal popup dialog to easily adjust ambient audios
    if (expandSoundDialog) {
        AlertDialog(
            onDismissRequest = { expandSoundDialog = false },
            containerColor = FocusCardBg,
            title = {
                Text(
                    text = "Ambient Soundscape",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FocusTextPrimaryColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Sound Options List
                    soundOptions.forEach { sound ->
                        val isSelected = sound == activeSound
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) FocusPillBg else Color.Transparent)
                                .clickable { viewModel.selectAmbientSound(sound) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("sound_item_$sound"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sound,
                                color = if (isSelected) FocusAccentColor else FocusTextPrimaryColor,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = FocusAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Volume controller slider
                    Text(
                        text = "Soundscape Volume",
                        fontSize = 12.sp,
                        color = FocusTextSecondaryColor,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = soundVolume,
                        onValueChange = { viewModel.updateAmbientVolume(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = FocusAccentColor,
                            activeTrackColor = FocusAccentColor,
                            inactiveTrackColor = FocusPillBg
                        ),
                        modifier = Modifier.testTag("sound_volume_slider")
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { expandSoundDialog = false }) {
                    Text(text = "CLOSE", color = FocusAccentColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- HISTORIC STATS TAB ---

@Composable
fun StatsTabContent(viewModel: FocusViewModel) {
    val sessions by viewModel.focusSessions.collectAsState()
    val completedMin by viewModel.dailyCompletedMinutes.collectAsState()
    val dailyGoalMin by viewModel.dailyGoalMinutes.collectAsState()
    val streakCount by viewModel.currentStreak.collectAsState()
    val efficiency by viewModel.efficiencyPercent.collectAsState()

    val totalWorkSessions = sessions.filter { it.mode == TimerMode.POMODORO.name }
    val completedWorkCount = totalWorkSessions.count { it.completed }
    val totalTimeSeconds = totalWorkSessions.filter { it.completed }.sumOf { it.durationSeconds }
    val totalTimeHoursStr = String.format(Locale.getDefault(), "%.1f", totalTimeSeconds.toFloat() / 3600f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Heading
        item {
            Text(
                text = "Productivity Insights",
                fontSize = 22.sp,
                color = FocusTextPrimaryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "A record of your focus and dedication.",
                fontSize = 12.sp,
                color = FocusTextSecondaryColor
            )
        }

        // Metrics Grid Grid Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus Hours Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusCardBg)
                        .border(1.dp, FocusBorderColor, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "TOTAL HOURS", fontSize = 9.sp, color = FocusTextSecondaryColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$totalTimeHoursStr hrs", fontSize = 20.sp, color = FocusAccentColor, fontWeight = FontWeight.Bold)
                }

                // Completed Pomodoros
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusCardBg)
                        .border(1.dp, FocusBorderColor, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "COMPLETED", fontSize = 9.sp, color = FocusTextSecondaryColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$completedWorkCount / ${totalWorkSessions.size}", fontSize = 20.sp, color = FocusTextPrimaryColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        // History Log List Header
        item {
            Text(
                text = "Focus Activity Logs",
                fontSize = 15.sp,
                color = FocusTextPrimaryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Empty logs placeholder or Items
        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "⏳", fontSize = 28.sp)
                        Text(
                            text = "No focus records yet.",
                            fontSize = 13.sp,
                            color = FocusTextSecondaryColor
                        )
                    }
                }
            }
        } else {
            items(sessions) { log ->
                val timeFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(log.timestamp))
                val minutesFocused = log.durationSeconds / 60
                val targetMinVal = log.targetSeconds / 60

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusCardBg)
                        .border(1.dp, FocusBorderColor, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (log.completed) FocusAccentColor else Color(0xFFEF5350))
                            )
                            Text(
                                text = log.mode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusTextPrimaryColor,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = timeFormatted,
                            fontSize = 11.sp,
                            color = FocusTextSecondaryColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (log.completed) "$minutesFocused min" else "Aborted ($minutesFocused m)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.completed) FocusTextPrimaryColor else FocusTextSecondaryColor
                        )
                        Text(
                            text = "Target: $targetMinVal min",
                            fontSize = 9.sp,
                            color = FocusTextSecondaryColor
                        )
                    }
                }
            }
        }
    }
}

// --- ADJUSTABLE SETTINGS TAB ---

@Composable
fun SettingsTabContent(viewModel: FocusViewModel) {
    val pomoByMin by viewModel.pomodoroDuration.collectAsState()
    val shortByMin by viewModel.shortBreakDuration.collectAsState()
    val longByMin by viewModel.longBreakDuration.collectAsState()
    val dailyGoalMin by viewModel.dailyGoalMinutes.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences",
                fontSize = 22.sp,
                color = FocusTextPrimaryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Customize your Pomodoro cycles and goals.",
                fontSize = 12.sp,
                color = FocusTextSecondaryColor
            )
        }

        // Pomodoro Length Slider
        item {
            SettingSliderCard(
                title = "Pomodoro Work Duration",
                subtitle = "Active focus interval",
                value = pomoByMin.toFloat(),
                range = 5f..60f,
                steps = 11, // steps of 5 mins (5, 10, 15, ..., 60)
                displaySuffix = "mins",
                onValueChange = { viewModel.updatePomodoroDuration(it.toLong()) },
                tag = "pomo_slider"
            )
        }

        // Short Break Length Slider
        item {
            SettingSliderCard(
                title = "Short Break Interval",
                subtitle = "Brief breathing break",
                value = shortByMin.toFloat(),
                range = 1f..15f,
                steps = 14, // 1 min steps
                displaySuffix = "mins",
                onValueChange = { viewModel.updateShortBreakDuration(it.toLong()) },
                tag = "short_slider"
            )
        }

        // Long Break Length Slider
        item {
            SettingSliderCard(
                title = "Long Break Interval",
                subtitle = "Longer rest break",
                value = longByMin.toFloat(),
                range = 5f..30f,
                steps = 5, // 5 min steps
                displaySuffix = "mins",
                onValueChange = { viewModel.updateLongBreakDuration(it.toLong()) },
                tag = "long_slider"
            )
        }

        // Daily study goal in hours Slider
        item {
            SettingSliderCard(
                title = "Daily Target Study Goal",
                subtitle = "Set hours of dedicated focus to aim for",
                value = (dailyGoalMin.toFloat() / 60f),
                range = 1f..12f,
                steps = 11, // 1 hour steps
                displaySuffix = "hours",
                onValueChange = { viewModel.updateDailyGoal((it * 60).toInt()) },
                tag = "goal_slider"
            )
        }

        // Reset Data Logs Database
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showClearConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFEF5350)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("clear_logs_button")
            ) {
                Text(text = "CLEAR ALL COMPLETED LOGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = FocusCardBg,
            title = {
                Text(text = "Are you sure?", color = FocusTextPrimaryColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "This will permanently erase all focus record logs and statistics. Streaks will reset immediately.",
                    color = FocusTextSecondaryColor,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_clear")
                ) {
                    Text(text = "YES, ERASE", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = "CANCEL", color = FocusAccentColor)
                }
            }
        )
    }
}

@Composable
fun SettingSliderCard(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    displaySuffix: String,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FocusCardBg)
            .border(1.dp, FocusBorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FocusTextPrimaryColor)
                Text(text = subtitle, fontSize = 11.sp, color = FocusTextSecondaryColor, lineHeight = 14.sp)
            }
            Text(
                text = if (displaySuffix == "hours") {
                    String.format(Locale.US, "%.1f %s", value, displaySuffix)
                } else {
                    "${value.toInt()} $displaySuffix"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = FocusAccentColor,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = FocusAccentColor,
                activeTrackColor = FocusAccentColor,
                inactiveTrackColor = FocusPillBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

// --- BOTTOM TAB NAVIGATION BAR ---

@Composable
fun BottomNavBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FocusBg,
        border = BorderStroke(1.dp, FocusPillBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                TabItem("⏱", "FOCUS", 0),
                TabItem("📊", "STATS", 1),
                TabItem("⚙️", "SETTINGS", 2)
            )

            tabs.forEach { tab ->
                val isSelected = activeTab == tab.index
                val optScale = if (isSelected) 1f else 0.6f

                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab.index) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("nav_tab_${tab.label.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = tab.emoji,
                        fontSize = 24.sp,
                        color = if (isSelected) FocusAccentColor else FocusTextSecondaryColor.copy(alpha = optScale)
                    )
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) FocusAccentColor else FocusTextSecondaryColor.copy(alpha = optScale),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

data class TabItem(val emoji: String, val label: String, val index: Int)
