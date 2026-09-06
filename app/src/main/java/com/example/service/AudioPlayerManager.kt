package com.example.service

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var currentPlayingFilePath: String = ""
    private var isSimulatedPlayback = false
    private var simulatedStartTime = 0L
    private var simulatedDuration = 0L

    fun prepareAndPlay(filePath: String, fallbackDurationMs: Long = 60000L) {
        stop()
        currentPlayingFilePath = filePath
        val file = File(filePath)

        if (file.exists() && file.length() > 0) {
            try {
                isSimulatedPlayback = false
                val player = MediaPlayer().apply {
                    setDataSource(context, Uri.fromFile(file))
                    prepare()
                    _totalDurationMs.value = duration.toLong()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _currentPositionMs.value = 0L
                        progressJob?.cancel()
                    }
                }
                mediaPlayer = player
                applySpeed(_playbackSpeed.value)
                player.start()
                _isPlaying.value = true
                startProgressTracker()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Simulated playback for demo records or when audio device is offline
        isSimulatedPlayback = true
        simulatedDuration = if (fallbackDurationMs > 0) fallbackDurationMs else 60000L
        _totalDurationMs.value = simulatedDuration
        _currentPositionMs.value = 0L
        _isPlaying.value = true
        simulatedStartTime = System.currentTimeMillis()
        startSimulatedProgressTracker()
    }

    fun togglePlayPause(filePath: String = "", fallbackDurationMs: Long = 60000L) {
        if (_isPlaying.value) {
            pause()
        } else {
            if (mediaPlayer != null || isSimulatedPlayback) {
                resume()
            } else {
                prepareAndPlay(filePath, fallbackDurationMs)
            }
        }
    }

    fun pause() {
        if (isSimulatedPlayback) {
            _isPlaying.value = false
            progressJob?.cancel()
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    progressJob?.cancel()
                }
            }
        }
    }

    fun resume() {
        if (isSimulatedPlayback) {
            _isPlaying.value = true
            startSimulatedProgressTracker()
        } else {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _totalDurationMs.value)
        _currentPositionMs.value = clamped
        if (!isSimulatedPlayback) {
            try {
                mediaPlayer?.seekTo(clamped.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun forward10Seconds() {
        seekTo(_currentPositionMs.value + 10000L)
    }

    fun rewind10Seconds() {
        seekTo(_currentPositionMs.value - 10000L)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    val params = it.playbackParams
                    params.speed = speed
                    it.playbackParams = params
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPositionMs.value = it.currentPosition.toLong()
                    }
                }
                delay(100)
            }
        }
    }

    private fun startSimulatedProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val step = (100 * _playbackSpeed.value).toLong()
                val next = _currentPositionMs.value + step
                if (next >= _totalDurationMs.value) {
                    _currentPositionMs.value = 0L
                    _isPlaying.value = false
                    break
                } else {
                    _currentPositionMs.value = next
                }
                delay(100)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
