package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CatSoundEngine
import com.example.audio.CatSoundType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppMode {
    MAIN_CAT,          // The main cat screen with tap reaction
    PRANK_TIMER,       // Countdown stealth bomb
    TAP_TRAP,          // Innocent sleeping cat screen waiting to be tapped
    SOUNDBOARD         // Grid of all funny cat sounds
}

data class CatUiState(
    val appMode: AppMode = AppMode.MAIN_CAT,
    val isPlayingSound: Boolean = false,
    val currentSoundType: CatSoundType? = null,
    val meowCount: Int = 0,
    val isLoopActive: Boolean = false,
    val loopIntervalSec: Int = 2,
    val speechBubbleText: String? = null,
    val isCatShocked: Boolean = false,
    // Timer Prank
    val isTimerRunning: Boolean = false,
    val timerRemainingSec: Int = 0,
    val isTimerTriggered: Boolean = false,
    // Trap Mode
    val isTrapTriggered: Boolean = false,
    // Volume
    val currentVolume: Int = 10,
    val maxVolume: Int = 15,
    val exitAttemptCount: Int = 0
)

class CatPrankViewModel(application: Application) : AndroidViewModel(application) {
    val soundEngine = CatSoundEngine(application)

    private val _uiState = MutableStateFlow(
        CatUiState(
            currentVolume = soundEngine.getCurrentVolumeLevel(),
            maxVolume = soundEngine.getMaxVolumeLevel()
        )
    )
    val uiState: StateFlow<CatUiState> = _uiState.asStateFlow()

    private var loopJob: Job? = null
    private var timerJob: Job? = null
    private var shockResetJob: Job? = null

    init {
        refreshVolume()
    }

    fun refreshVolume() {
        _uiState.update {
            it.copy(
                currentVolume = soundEngine.getCurrentVolumeLevel(),
                maxVolume = soundEngine.getMaxVolumeLevel()
            )
        }
    }

    fun setVolume(level: Int) {
        soundEngine.setMediaVolume(level)
        refreshVolume()
    }

    fun setMaxVolume() {
        soundEngine.setMaxVolume()
        refreshVolume()
    }

    fun triggerCatTap() {
        // Pick a sound depending on context or random funny meow
        val sounds = listOf(
            CatSoundType.LOUD_MEOW,
            CatSoundType.SCREAM_CAT,
            CatSoundType.DRAMATIC_HUH,
            CatSoundType.TRILL_CHIRP,
            CatSoundType.OPERA_CAT
        )
        val selected = sounds.random()
        playSound(selected)
    }

    fun playSound(type: CatSoundType) {
        triggerShockVisual(type.bubbleText)

        _uiState.update {
            it.copy(
                isPlayingSound = true,
                currentSoundType = type,
                meowCount = it.meowCount + 1
            )
        }

        soundEngine.playSound(
            type = type,
            onStarted = {
                _uiState.update { it.copy(isPlayingSound = true, currentSoundType = type) }
            },
            onFinished = {
                _uiState.update { it.copy(isPlayingSound = false) }
            }
        )
    }

    private fun triggerShockVisual(bubble: String) {
        shockResetJob?.cancel()
        _uiState.update {
            it.copy(
                isCatShocked = true,
                speechBubbleText = bubble
            )
        }
        shockResetJob = viewModelScope.launch {
            delay(1500)
            _uiState.update {
                it.copy(
                    isCatShocked = false,
                    speechBubbleText = null
                )
            }
        }
    }

    fun toggleLoop() {
        val newState = !_uiState.value.isLoopActive
        _uiState.update { it.copy(isLoopActive = newState) }
        loopJob?.cancel()

        if (newState) {
            loopJob = viewModelScope.launch {
                while (isActive) {
                    val sound = listOf(
                        CatSoundType.LOUD_MEOW,
                        CatSoundType.SCREAM_CAT,
                        CatSoundType.DRAMATIC_HUH,
                        CatSoundType.OPERA_CAT
                    ).random()
                    playSound(sound)
                    delay((_uiState.value.loopIntervalSec * 1000L).coerceAtLeast(1500L))
                }
            }
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        _uiState.update { it.copy(isLoopActive = false) }
    }

    // Timer Prank
    fun startTimerPrank(seconds: Int) {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isTimerRunning = true,
                timerRemainingSec = seconds,
                isTimerTriggered = false
            )
        }

        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(timerRemainingSec = remaining) }
            }
            if (isActive) {
                // Trigger prank! Max volume + scream cat
                soundEngine.setMaxVolume()
                refreshVolume()
                _uiState.update {
                    it.copy(
                        isTimerRunning = false,
                        isTimerTriggered = true
                    )
                }
                playSound(CatSoundType.SCREAM_CAT)
            }
        }
    }

    fun cancelTimerPrank() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isTimerRunning = false,
                timerRemainingSec = 0,
                isTimerTriggered = false
            )
        }
    }

    // Tap Trap Mode
    fun triggerTrapTouched() {
        if (!_uiState.value.isTrapTriggered) {
            soundEngine.setMaxVolume()
            refreshVolume()
            _uiState.update { it.copy(isTrapTriggered = true) }
            playSound(CatSoundType.SCREAM_CAT)
        }
    }

    fun resetTrap() {
        _uiState.update { it.copy(isTrapTriggered = false) }
    }

    fun setAppMode(mode: AppMode) {
        _uiState.update { it.copy(appMode = mode) }
    }

    fun recordExitAttempt(): Boolean {
        // Increment attempt count
        val newCount = _uiState.value.exitAttemptCount + 1
        _uiState.update { it.copy(exitAttemptCount = newCount) }
        // Play funny huh meow on exit attempt
        playSound(CatSoundType.DRAMATIC_HUH)
        return newCount >= 2
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
        timerJob?.cancel()
        soundEngine.release()
    }
}
