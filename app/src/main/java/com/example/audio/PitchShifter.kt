package com.example.audio

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Real-time pitch shifting for voice changing, in pure Kotlin.
 *
 * Pitch is shifted without altering duration by composing two stages:
 *   1. [TimeStretcher] stretches the signal by `pitchFactor` (WSOLA), making it longer.
 *   2. [Resampler] reads it back at `pitchFactor` speed, restoring the original length
 *      and raising the pitch by exactly that factor.
 *
 * Audio stays in the float domain between the stages and is only converted back to 16-bit
 * PCM on the way out. Both stages are streaming, so this can sit in a live capture loop.
 *
 * Deliberately dependency-free: TarsosDSP is GPL-3.0 and SoundTouch needs the NDK.
 */
class PitchShifter(
  sampleRate: Int,
  pitchFactor: Float,
) {
  private val stretcher = TimeStretcher(stretchFactor = pitchFactor, sampleRate = sampleRate)
  private val resampler = Resampler(step = pitchFactor)

  /** 1.0 = unchanged, 2.0 = one octave up, 0.5 = one octave down. */
  var pitchFactor: Float = pitchFactor.coerceIn(MIN_FACTOR, MAX_FACTOR)
    set(value) {
      val clamped = value.coerceIn(MIN_FACTOR, MAX_FACTOR)
      field = clamped
      stretcher.stretchFactor = clamped
      resampler.step = clamped
    }

  /** Feeds one block of mono PCM and returns whatever output is ready. May be empty. */
  fun process(input: ShortArray): ShortArray = resampler.process(stretcher.process(input))

  fun reset() {
    stretcher.reset()
    resampler.reset()
  }

  companion object {
    const val MIN_FACTOR = 0.5f
    const val MAX_FACTOR = 2.0f
  }
}

/**
 * WSOLA (Waveform Similarity Overlap-Add) time stretching.
 *
 * Each output frame is cross-correlated against the tail of the previous frame within a
 * seek window, so the splice lands on a similar waveform phase. That is what keeps voice
 * intelligible instead of warbling, which naive overlap-add produces.
 *
 * Consumes 16-bit PCM, emits normalised floats.
 */
class TimeStretcher(
  stretchFactor: Float,
  @Suppress("unused") private val sampleRate: Int,
  private val frameSize: Int = DEFAULT_FRAME,
  private val seekWindow: Int = DEFAULT_SEEK,
) {
  private val overlap = frameSize / 2
  private val window = FloatArray(overlap) { 0.5f - 0.5f * cos(2.0 * Math.PI * it / overlap).toFloat() }

  /** Tail of the previous synthesis frame, awaiting overlap-add with the next one. */
  private var tail = FloatArray(overlap)
  private var primed = false

  private var buffer = FloatArray(frameSize * 4)
  private var buffered = 0
  private var readPos = 0.0

  var stretchFactor: Float = stretchFactor.coerceIn(0.25f, 4.0f)
    set(value) {
      field = value.coerceIn(0.25f, 4.0f)
    }

  /** Analysis hop. Smaller than the synthesis hop stretches the signal longer. */
  private val analysisHop: Double
    get() = overlap / stretchFactor.toDouble()

  fun process(input: ShortArray): FloatArray {
    append(input)

    val out = FloatArrayBuilder()
    // Every read must stay inside the buffer: the seek search reaches readPos + seekWindow,
    // and the frame plus its following tail extends another 2 * overlap beyond that.
    val needed = seekWindow + 2 * overlap
    while (readPos.toInt() + needed < buffered) {
      val base = readPos.toInt()

      if (!primed) {
        System.arraycopy(buffer, base, tail, 0, overlap)
        primed = true
      } else {
        val offset = bestOffset(base)
        val frame = FloatArray(overlap)
        for (i in 0 until overlap) {
          val w = window[i]
          frame[i] = tail[i] * (1f - w) + buffer[base + offset + i] * w
        }
        out.append(frame)
        System.arraycopy(buffer, base + offset + overlap, tail, 0, overlap)
      }
      readPos += analysisHop
    }

    compact()
    return out.toFloatArray()
  }

  /**
   * Finds the shift within [0, seekWindow] whose waveform best matches [tail], using
   * normalised cross-correlation.
   */
  private fun bestOffset(base: Int): Int {
    var bestOffset = 0
    var bestScore = -Float.MAX_VALUE
    for (offset in 0..seekWindow) {
      var dot = 0f
      var energy = 1e-6f
      var i = 0
      while (i < overlap) {
        val s = buffer[base + offset + i]
        dot += tail[i] * s
        energy += s * s
        i += CORRELATION_STRIDE
      }
      val score = dot / sqrt(energy)
      if (score > bestScore) {
        bestScore = score
        bestOffset = offset
      }
    }
    return bestOffset
  }

  private fun append(input: ShortArray) {
    ensureCapacity(buffered + input.size)
    for (s in input) buffer[buffered++] = s / 32768f
  }

  private fun ensureCapacity(needed: Int) {
    if (needed <= buffer.size) return
    var size = buffer.size
    while (size < needed) size *= 2
    buffer = buffer.copyOf(size)
  }

  /** Drops consumed samples and rebases [readPos] so the buffer cannot grow without bound. */
  private fun compact() {
    val consumed = readPos.toInt()
    if (consumed <= 0) return
    val keep = buffered - consumed
    System.arraycopy(buffer, consumed, buffer, 0, keep)
    buffered = keep
    readPos -= consumed
  }

  fun reset() {
    buffered = 0
    readPos = 0.0
    primed = false
    tail = FloatArray(overlap)
  }

  companion object {
    const val DEFAULT_FRAME = 1024
    const val DEFAULT_SEEK = 256
    /** Correlating every 4th sample is ~4x cheaper and picks the same offset in practice. */
    private const val CORRELATION_STRIDE = 4
  }
}

/**
 * Streaming linear-interpolation resampler. Reading faster than 1.0 raises pitch.
 * Consumes normalised floats, emits 16-bit PCM.
 */
class Resampler(step: Float) {
  var step: Float = step.coerceIn(0.25f, 4.0f)
    set(value) {
      field = value.coerceIn(0.25f, 4.0f)
    }

  private var carry = FloatArray(0)
  private var pos = 0.0

  fun process(input: FloatArray): ShortArray {
    if (input.isEmpty() && carry.isEmpty()) return ShortArray(0)

    val data = if (carry.isEmpty()) input else carry + input
    val out = FloatArrayBuilder()

    while (pos + 1 < data.size) {
      val i = floor(pos).toInt()
      val frac = (pos - i).toFloat()
      out.append(data[i] * (1f - frac) + data[i + 1] * frac)
      pos += step
    }

    // Keep the sample the next interpolation still needs, and rebase the read position.
    val consumed = floor(pos).toInt().coerceIn(0, data.size)
    carry = data.copyOfRange(consumed, data.size)
    pos -= consumed
    return out.toShortArray()
  }

  fun reset() {
    carry = FloatArray(0)
    pos = 0.0
  }
}

/** Growable float accumulator that converts to clipped 16-bit PCM on demand. */
internal class FloatArrayBuilder {
  private var data = FloatArray(2048)
  private var size = 0

  fun append(value: Float) {
    if (size == data.size) data = data.copyOf(size * 2)
    data[size++] = value
  }

  fun append(values: FloatArray) {
    if (size + values.size > data.size) {
      var cap = data.size
      while (cap < size + values.size) cap *= 2
      data = data.copyOf(cap)
    }
    System.arraycopy(values, 0, data, size, values.size)
    size += values.size
  }

  fun toShortArray(): ShortArray = ShortArray(size) {
    (data[it].coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
  }

  fun toFloatArray(): FloatArray = data.copyOf(size)
}
