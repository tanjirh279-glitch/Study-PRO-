package com.example.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientSynthesizer
import com.example.data.AppDatabase
import com.example.data.FocusRepository
import com.example.data.FocusSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TimerState {
    IDLE, RUNNING, PAUSED
}

enum class TimerMode {
    POMODORO, SHORT_BREAK, LONG_BREAK
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FocusRepository(database.focusSessionDao())
    private val sharedPreferences = application.getSharedPreferences("study_focus_prefs", Context.MODE_PRIVATE)
    private val ambientSynthesizer = AmbientSynthesizer()

    // --- State Observables ---
    private val _timerValue = MutableStateFlow(1500L) // in seconds
    val timerValue: StateFlow<Long> = _timerValue.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.POMODORO)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    // Custom configuration states (minutes)
    private val _pomodoroDuration = MutableStateFlow(sharedPreferences.getLong("pomodoro_duration", 25))
    val pomodoroDuration: StateFlow<Long> = _pomodoroDuration.asStateFlow()

    private val _shortBreakDuration = MutableStateFlow(sharedPreferences.getLong("short_break_duration", 5))
    val shortBreakDuration: StateFlow<Long> = _shortBreakDuration.asStateFlow()

    private val _longBreakDuration = MutableStateFlow(sharedPreferences.getLong("long_break_duration", 15))
    val longBreakDuration: StateFlow<Long> = _longBreakDuration.asStateFlow()

    // Daily study target goal in minutes (default 240m / 4 hours)
    private val _dailyGoalMinutes = MutableStateFlow(sharedPreferences.getInt("daily_goal_minutes", 240))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    // Ambient audio options
    val ambientSounds = listOf("Summer Rain & Birds", "Forest Stream", "Warm Fireplace", "Cosmic Alpha Drone")
    private val _selectedAmbientSound = MutableStateFlow(sharedPreferences.getString("selected_ambient", "Summer Rain & Birds") ?: "Summer Rain & Birds")
    val selectedAmbientSound: StateFlow<String> = _selectedAmbientSound.asStateFlow()

    private val _isAmbientPlaying = MutableStateFlow(false)
    val isAmbientPlaying: StateFlow<Boolean> = _isAmbientPlaying.asStateFlow()

    private val _ambientVolume = MutableStateFlow(sharedPreferences.getFloat("ambient_volume", 0.5f))
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()

    // Quotes
    val quotes = listOf(
        "\"The secret of your future is hidden in your daily routine.\"",
        "\"Focus is a muscle, and you build it second by second.\"",
        "\"Deep work is not a chore; it is a state of high art.\"",
        "\"Do not wait for motivation. Action breeds motivation.\"",
        "\"Your mind is for having ideas, not holding them. Focus on one thing.\"",
        "\"Quiet the mind, block the noise, and let your craft speak.\"",
        "\"Small daily improvements over time lead to stunning results.\""
    )
    private val _currentQuoteIndex = MutableStateFlow(0)
    val currentQuote: StateFlow<String> = _currentQuoteIndex.map { quotes[it] }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), quotes[0])

    // DB session stream
    val focusSessions: StateFlow<List<FocusSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current statistics compiled from sessions
    val dailyCompletedMinutes: StateFlow<Long> = focusSessions.map { sessions ->
        calculateDailyCompletedMinutes(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentStreak: StateFlow<Int> = focusSessions.map { sessions ->
        calculateStreak(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val efficiencyPercent: StateFlow<Int> = focusSessions.map { sessions ->
        calculateEfficiency(sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    // Countdown job
    private var timerJob: Job? = null
    private var lastTickTime = 0L

    init {
        resetTimer(TimerMode.POMODORO)
    }

    // --- Timer Controls ---
    fun startTimer() {
        if (_timerState.value == TimerState.RUNNING) return

        _timerState.value = TimerState.RUNNING
        lastTickTime = SystemClock.elapsedRealtime()
        
        timerJob = viewModelScope.launch {
            while (_timerState.value == TimerState.RUNNING && _timerValue.value > 0) {
                delay(1000)
                _timerValue.value = (_timerValue.value - 1).coerceAtLeast(0)
                if (_timerValue.value == 0L) {
                    completeSession()
                }
            }
        }
    }

    fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
    }

    fun stopTimer() {
        pauseTimer()
        val targetInSec = getModeDurationSeconds(_timerMode.value)
        val elapsed = targetInSec - _timerValue.value
        
        // Log partial session if useful (only if active work session exceeds 10s)
        if (_timerMode.value == TimerMode.POMODORO && elapsed >= 10L) {
            viewModelScope.launch {
                repository.insertSession(
                    FocusSession(
                        durationSeconds = elapsed,
                        targetSeconds = targetInSec,
                        mode = _timerMode.value.name,
                        completed = false
                    )
                )
            }
        }
        
        _timerState.value = TimerState.IDLE
        resetTimer(_timerMode.value)
    }

    fun skipTimer() {
        stopTimer()
        // Auto-switch to break if Pomodoro finished, or vice versa
        val nextMode = when (_timerMode.value) {
            TimerMode.POMODORO -> TimerMode.SHORT_BREAK
            TimerMode.SHORT_BREAK, TimerMode.LONG_BREAK -> TimerMode.POMODORO
        }
        resetTimer(nextMode)
    }

    private fun completeSession() {
        _timerState.value = TimerState.IDLE
        timerJob?.cancel()

        val modeAtEnd = _timerMode.value
        val targetInSec = getModeDurationSeconds(modeAtEnd)

        viewModelScope.launch {
            repository.insertSession(
                FocusSession(
                    durationSeconds = targetInSec,
                    targetSeconds = targetInSec,
                    mode = modeAtEnd.name,
                    completed = true
                )
            )
            // Cycle visual quote
            cycleQuote()
            // Auto switch modes to give immediate break or prompt next focus
            val nextMode = when (modeAtEnd) {
                TimerMode.POMODORO -> TimerMode.SHORT_BREAK
                else -> TimerMode.POMODORO
            }
            resetTimer(nextMode)
        }
    }

    fun resetTimer(mode: TimerMode) {
        _timerMode.value = mode
        _timerValue.value = getModeDurationSeconds(mode)
    }

    private fun getModeDurationSeconds(mode: TimerMode): Long {
        return when (mode) {
            TimerMode.POMODORO -> _pomodoroDuration.value * 60
            TimerMode.SHORT_BREAK -> _shortBreakDuration.value * 60
            TimerMode.LONG_BREAK -> _longBreakDuration.value * 60
        }
    }

    // --- Configurations & Settings ---
    fun updatePomodoroDuration(mins: Long) {
        val safeMins = mins.coerceIn(1, 180)
        _pomodoroDuration.value = safeMins
        sharedPreferences.edit().putLong("pomodoro_duration", safeMins).apply()
        if (_timerState.value == TimerState.IDLE && _timerMode.value == TimerMode.POMODORO) {
            resetTimer(TimerMode.POMODORO)
        }
    }

    fun updateShortBreakDuration(mins: Long) {
        val safeMins = mins.coerceIn(1, 60)
        _shortBreakDuration.value = safeMins
        sharedPreferences.edit().putLong("short_break_duration", safeMins).apply()
        if (_timerState.value == TimerState.IDLE && _timerMode.value == TimerMode.SHORT_BREAK) {
            resetTimer(TimerMode.SHORT_BREAK)
        }
    }

    fun updateLongBreakDuration(mins: Long) {
        val safeMins = mins.coerceIn(1, 120)
        _longBreakDuration.value = safeMins
        sharedPreferences.edit().putLong("long_break_duration", safeMins).apply()
        if (_timerState.value == TimerState.IDLE && _timerMode.value == TimerMode.LONG_BREAK) {
            resetTimer(TimerMode.LONG_BREAK)
        }
    }

    fun updateDailyGoal(mins: Int) {
        val safeMins = mins.coerceIn(10, 1440)
        _dailyGoalMinutes.value = safeMins
        sharedPreferences.edit().putInt("daily_goal_minutes", safeMins).apply()
    }

    // --- Ambient Audio API ---
    fun toggleAmbientAudio() {
        if (_isAmbientPlaying.value) {
            ambientSynthesizer.stop()
            _isAmbientPlaying.value = false
        } else {
            ambientSynthesizer.start(_selectedAmbientSound.value, _ambientVolume.value)
            _isAmbientPlaying.value = true
        }
    }

    fun selectAmbientSound(sound: String) {
        if (sound in ambientSounds) {
            _selectedAmbientSound.value = sound
            sharedPreferences.edit().putString("selected_ambient", sound).apply()
            if (_isAmbientPlaying.value) {
                ambientSynthesizer.start(sound, _ambientVolume.value)
            }
        }
    }

    fun updateAmbientVolume(vol: Float) {
        val safeVol = vol.coerceIn(0.0f, 1.0f)
        _ambientVolume.value = safeVol
        sharedPreferences.edit().putFloat("ambient_volume", safeVol).apply()
        if (_isAmbientPlaying.value) {
            ambientSynthesizer.setVolume(safeVol)
        }
    }

    // --- Quotes Carousel ---
    fun cycleQuote() {
        _currentQuoteIndex.value = (_currentQuoteIndex.value + 1) % quotes.size
    }

    // --- DB clear ---
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // --- Cleanup ---
    override fun onCleared() {
        super.onCleared()
        ambientSynthesizer.stop()
        timerJob?.cancel()
    }

    // --- Calculation Helpers ---
    private fun calculateDailyCompletedMinutes(sessions: List<FocusSession>): Long {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return sessions
            .filter { it.mode == TimerMode.POMODORO.name && it.completed }
            .filter {
                val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
                dayStr == todayStr
            }
            .sumOf { it.durationSeconds } / 60
    }

    private fun calculateStreak(sessions: List<FocusSession>): Int {
        val pms = sessions
            .filter { it.mode == TimerMode.POMODORO.name && it.completed }
            .map { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) }
            .toSet()

        if (pms.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        var streak = 0
        var checkDateStr = sdf.format(cal.time)

        // If today has session, check from today; if not, check if yesterday had session
        if (pms.contains(checkDateStr)) {
            streak++
            cal.add(Calendar.DATE, -1)
            checkDateStr = sdf.format(cal.time)
            while (pms.contains(checkDateStr)) {
                streak++
                cal.add(Calendar.DATE, -1)
                checkDateStr = sdf.format(cal.time)
            }
        } else {
            cal.add(Calendar.DATE, -1)
            checkDateStr = sdf.format(cal.time)
            if (pms.contains(checkDateStr)) {
                streak++
                cal.add(Calendar.DATE, -1)
                checkDateStr = sdf.format(cal.time)
                while (pms.contains(checkDateStr)) {
                    streak++
                    cal.add(Calendar.DATE, -1)
                    checkDateStr = sdf.format(cal.time)
                }
            }
        }
        return streak
    }

    private fun calculateEfficiency(sessions: List<FocusSession>): Int {
        val workSessions = sessions.filter { it.mode == TimerMode.POMODORO.name }
        if (workSessions.isEmpty()) return 100

        val completed = workSessions.count { it.completed }
        return ((completed.toFloat() / workSessions.size) * 100).toInt()
    }
}
