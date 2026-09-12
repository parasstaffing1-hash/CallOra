package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread

/**
 * Captures the microphone, pitch-shifts it in real time, and hands the altered PCM to a sink.
 *
 * ## What this can and cannot do
 *
 * This changes audio that **CallOra itself owns**. It cannot alter what the far end hears on a
 * WhatsApp or cellular call: WhatsApp opens the microphone in its own process, and the cellular
 * uplink runs microphone -> modem without passing through the application layer. Android exposes
 * no API to inject audio into either path. Changing a live call for the far end therefore
 * requires CallOra to place the call itself, which is what [onFrame] exists for - a VoIP stack's
 * audio device module reads shifted frames from there and encodes them outbound.
 *
 * [SinkMode.MONITOR] routes to the local speaker/headset instead, for auditioning a preset.
 *
 * ## Threading contract
 *
 * [start] and [stop] are safe to call from any thread and are idempotent. All audio work happens
 * on a single internal capture thread; the DSP objects are touched only by that thread, so preset
 * changes from the UI are staged and applied at a block boundary rather than mutating filter
 * state mid-block.
 */
class VoiceChangerEngine(
  private val context: Context,
  private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
  effect: VoiceEffect = VoiceEffect.NONE,
) {

  enum class SinkMode {
    /** Play the shifted audio locally so the user can hear the preset. */
    MONITOR,

    /** Emit frames through [onFrame] only. The sink for a VoIP encoder. */
    CALLBACK,
  }

  /** Why [start] failed, for callers that need to tell the user something specific. */
  sealed interface StartResult {
    data object Started : StartResult
    data object AlreadyRunning : StartResult
    data object PermissionDenied : StartResult
    data class Unsupported(val reason: String) : StartResult
  }

  /**
   * Receives shifted 16-bit mono PCM at [sampleRate]. This is the injection point for a VoIP
   * audio device module. Called on the capture thread - do not block it.
   *
   * Set this before [start]; it is read once per block without synchronisation.
   */
  @Volatile
  var onFrame: ((ShortArray) -> Unit)? = null

  private val _level = MutableStateFlow(0f)

  /** Peak amplitude of the most recent block, 0..1, for waveform/level UI. */
  val level: StateFlow<Float> = _level.asStateFlow()

  private val _running = MutableStateFlow(false)
  val running: StateFlow<Boolean> = _running.asStateFlow()

  /**
   * Staged preset. The capture thread picks this up between blocks so the filter's internal
   * state is never mutated while a block is being processed.
   */
  @Volatile
  private var pendingEffect: VoiceEffect? = null

  @Volatile
  var effect: VoiceEffect = effect
    set(value) {
      field = value
      pendingEffect = value
    }

  private val shifter = PitchShifter(sampleRate, effect.pitchFactor)

  /** Guards start/stop transitions and the device handles they own. */
  private val lifecycleLock = Any()

  private var record: AudioRecord? = null
  private var track: AudioTrack? = null
  private var worker: Thread? = null
  private var aec: AcousticEchoCanceler? = null
  private var ns: NoiseSuppressor? = null

  private val audioManager =
    context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var focusRequest: AudioFocusRequest? = null

  /**
   * Something else took the audio route - an incoming call, a voice assistant. Holding the
   * microphone through that is both antisocial and, on many OEM builds, silently broken.
   */
  private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
    if (change == AudioManager.AUDIOFOCUS_LOSS ||
      change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
    ) {
      Log.i(TAG, "Audio focus lost (change=$change); stopping")
      // This callback lands on the main thread and stop() joins the capture thread, so
      // doing it inline would risk an ANR.
      thread(name = "$THREAD_NAME-focus-stop") { stop() }
    }
  }

  val isRunning: Boolean get() = _running.value

  /**
   * Starts capture. Caller must already hold RECORD_AUDIO.
   *
   * In [SinkMode.MONITOR] the caller should prefer a headset: routing shifted mic audio to the
   * loudspeaker creates an acoustic loop that hardware AEC only partly suppresses.
   */
  @SuppressLint("MissingPermission")
  fun start(mode: SinkMode = SinkMode.CALLBACK): StartResult = synchronized(lifecycleLock) {
    if (_running.value) return StartResult.AlreadyRunning

    val minIn = AudioRecord.getMinBufferSize(sampleRate, IN_CHANNEL, ENCODING)
    if (minIn <= 0) {
      return StartResult.Unsupported("No capture config at ${sampleRate}Hz")
    }

    val recorder = try {
      AudioRecord(
        // VOICE_COMMUNICATION engages the hardware AEC/AGC path, matching CallRecordingService.
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        sampleRate,
        IN_CHANNEL,
        ENCODING,
        maxOf(minIn, BLOCK_SAMPLES * BYTES_PER_SAMPLE * BUFFER_BLOCKS),
      )
    } catch (e: SecurityException) {
      Log.e(TAG, "RECORD_AUDIO not granted", e)
      return StartResult.PermissionDenied
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "Bad capture config", e)
      return StartResult.Unsupported(e.message ?: "Bad capture config")
    }

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      recorder.release()
      return StartResult.Unsupported("AudioRecord failed to initialise")
    }
    record = recorder
    attachEffects(recorder.audioSessionId)

    if (mode == SinkMode.MONITOR && !startMonitor()) {
      releaseAll()
      return StartResult.Unsupported("AudioTrack failed to initialise")
    }

    if (!requestFocus()) {
      releaseAll()
      return StartResult.Unsupported("Audio focus denied")
    }

    shifter.reset()
    shifter.pitchFactor = effect.pitchFactor
    pendingEffect = null

    return try {
      recorder.startRecording()
      _running.value = true
      worker = thread(name = THREAD_NAME, priority = Thread.MAX_PRIORITY) { loop(mode, recorder) }
      StartResult.Started
    } catch (e: IllegalStateException) {
      Log.e(TAG, "startRecording failed", e)
      releaseAll()
      StartResult.Unsupported(e.message ?: "startRecording failed")
    }
  }

  /**
   * Stops capture and releases devices. Blocks until the capture thread has exited, so the
   * native buffers are never freed while a read is still in flight.
   */
  fun stop() {
    val toJoin: Thread?
    synchronized(lifecycleLock) {
      if (!_running.value) {
        // start() may have failed part-way and left handles behind.
        releaseAll()
        return
      }
      _running.value = false
      // Stop the device first: this unblocks a capture thread parked in read(), so the join
      // below returns promptly instead of timing out and racing releaseAll().
      runCatching {
        record?.let { if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop() }
      }
      toJoin = worker
      worker = null
    }

    // Joined outside the lock so the capture thread can never deadlock against it.
    toJoin?.join(JOIN_TIMEOUT_MS)
    if (toJoin?.isAlive == true) {
      // Releasing native buffers under an in-flight read() would crash the process.
      Log.e(TAG, "Capture thread did not exit; leaking devices rather than crashing")
      synchronized(lifecycleLock) { record = null; track = null; aec = null; ns = null }
      _level.value = 0f
      return
    }

    synchronized(lifecycleLock) { releaseAll() }
    _level.value = 0f
  }

  private fun loop(mode: SinkMode, recorder: AudioRecord) {
    // Local handles: stop() may null the fields while this thread is still draining.
    val sink = track
    val buf = ShortArray(BLOCK_SAMPLES)

    while (_running.value) {
      val read = try {
        recorder.read(buf, 0, buf.size)
      } catch (e: IllegalStateException) {
        Log.w(TAG, "read() after stop", e)
        break
      }
      if (read <= 0) {
        // ERROR_INVALID_OPERATION means the device was stopped underneath us.
        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_DEAD_OBJECT) break
        continue
      }

      // Apply a staged preset only between blocks, never mid-filter.
      pendingEffect?.let {
        shifter.pitchFactor = it.pitchFactor
        pendingEffect = null
      }

      _level.value = peakOf(buf, read)

      val block = if (read == buf.size) buf else buf.copyOf(read)
      // NONE still goes through the shifter so latency stays constant when toggling presets.
      val shifted = shifter.process(block)
      if (shifted.isEmpty()) continue

      when (mode) {
        SinkMode.MONITOR -> sink?.write(shifted, 0, shifted.size)
        SinkMode.CALLBACK -> onFrame?.invoke(shifted)
      }
    }
  }

  private fun peakOf(block: ShortArray, count: Int): Float {
    var peak = 0
    for (i in 0 until count) {
      val v = if (block[i] < 0) -block[i].toInt() else block[i].toInt()
      if (v > peak) peak = v
    }
    return peak / 32768f
  }

  private fun startMonitor(): Boolean {
    val minOut = AudioTrack.getMinBufferSize(sampleRate, OUT_CHANNEL, ENCODING)
    if (minOut <= 0) return false
    val t = try {
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(sampleRate)
            .setChannelMask(OUT_CHANNEL)
            .build()
        )
        .setBufferSizeInBytes(maxOf(minOut, BLOCK_SAMPLES * BYTES_PER_SAMPLE * BUFFER_BLOCKS))
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    } catch (e: UnsupportedOperationException) {
      Log.e(TAG, "AudioTrack unsupported", e)
      return false
    }

    if (t.state != AudioTrack.STATE_INITIALIZED) {
      t.release()
      return false
    }
    track = t
    t.play()
    return true
  }

  /** Hardware AEC matters most in MONITOR mode, where output can feed back into the mic. */
  private fun attachEffects(sessionId: Int) {
    runCatching {
      if (AcousticEchoCanceler.isAvailable()) {
        aec = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
      }
      if (NoiseSuppressor.isAvailable()) {
        ns = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
      }
    }.onFailure { Log.w(TAG, "Audio effects unavailable", it) }
  }

  private fun requestFocus(): Boolean {
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()
    val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
      .setAudioAttributes(attrs)
      .setOnAudioFocusChangeListener(focusListener)
      .build()
    focusRequest = request
    return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  private fun abandonFocus() {
    focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
    focusRequest = null
  }

  /** Must be called with [lifecycleLock] held and the capture thread known to be stopped. */
  private fun releaseAll() {
    abandonFocus()
    runCatching { aec?.release() }; aec = null
    runCatching { ns?.release() }; ns = null
    runCatching {
      record?.let { if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop() }
      record?.release()
    }
    record = null
    runCatching {
      track?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop() }
      track?.release()
    }
    track = null
  }

  companion object {
    private const val TAG = "VoiceChangerEngine"
    private const val THREAD_NAME = "callora-voice-changer"

    /** 44.1 kHz is the most widely supported capture rate across devices. */
    const val DEFAULT_SAMPLE_RATE = 44100

    /** ~23 ms at 44.1 kHz. Small enough for conversation, large enough for stable WSOLA frames. */
    const val BLOCK_SAMPLES = 1024

    /** Generous enough to ride out scheduler jitter without adding audible latency. */
    private const val BUFFER_BLOCKS = 4
    private const val BYTES_PER_SAMPLE = 2

    /** A block is ~23 ms; this is many blocks of slack before we declare the thread wedged. */
    private const val JOIN_TIMEOUT_MS = 2000L

    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private const val IN_CHANNEL = AudioFormat.CHANNEL_IN_MONO
    private const val OUT_CHANNEL = AudioFormat.CHANNEL_OUT_MONO
  }
}
