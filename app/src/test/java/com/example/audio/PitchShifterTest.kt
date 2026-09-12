package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the shifter by measuring the fundamental frequency of the output, rather than
 * only asserting that it produced samples.
 */
class PitchShifterTest {

  private val sampleRate = 44100

  private fun sine(freq: Double, seconds: Double): ShortArray {
    val n = (sampleRate * seconds).toInt()
    return ShortArray(n) { i ->
      (sin(2.0 * PI * freq * i / sampleRate) * 20000).toInt().toShort()
    }
  }

  /**
   * Estimates the fundamental by autocorrelation, which tolerates the phase discontinuities
   * WSOLA splices introduce (zero-crossing counting does not).
   */
  private fun estimateFrequency(pcm: ShortArray): Double {
    // Analyse the steady-state middle, skipping the engine's warm-up and tail.
    val from = pcm.size / 4
    val len = pcm.size / 2
    val x = DoubleArray(len) { pcm[from + it] / 32768.0 }

    val minLag = sampleRate / 2000 // 2000 Hz ceiling
    val maxLag = sampleRate / 50 // 50 Hz floor

    var bestLag = minLag
    var bestScore = -Double.MAX_VALUE
    for (lag in minLag..maxLag) {
      var dot = 0.0
      var energy = 1e-9
      for (i in 0 until len - lag) {
        dot += x[i] * x[i + lag]
        energy += x[i + lag] * x[i + lag]
      }
      val score = dot / kotlin.math.sqrt(energy)
      if (score > bestScore) {
        bestScore = score
        bestLag = lag
      }
    }
    return sampleRate.toDouble() / bestLag
  }

  private fun runShifter(input: ShortArray, factor: Float): ShortArray {
    val shifter = PitchShifter(sampleRate, factor)
    val out = FloatArrayBuilder()
    val block = 1024
    var i = 0
    while (i < input.size) {
      val end = minOf(i + block, input.size)
      for (s in shifter.process(input.copyOfRange(i, end))) out.append(s / 32768f)
      i = end
    }
    return out.toShortArray()
  }

  @Test
  fun `control - measurement harness reads the input frequency correctly`() {
    assertEquals(220.0, estimateFrequency(sine(220.0, 1.0)), 5.0)
  }

  @Test
  fun `shifting up an octave doubles the fundamental`() {
    val out = runShifter(sine(220.0, 2.0), 2.0f)
    assertEquals(440.0, estimateFrequency(out), 20.0)
  }

  @Test
  fun `shifting down an octave halves the fundamental`() {
    val out = runShifter(sine(440.0, 2.0), 0.5f)
    assertEquals(220.0, estimateFrequency(out), 15.0)
  }

  @Test
  fun `unity factor leaves the fundamental unchanged`() {
    val out = runShifter(sine(300.0, 2.0), 1.0f)
    assertEquals(300.0, estimateFrequency(out), 10.0)
  }

  @Test
  fun `output length stays close to input length`() {
    val input = sine(220.0, 2.0)
    for (factor in listOf(0.6f, 1.0f, 1.7f)) {
      val out = runShifter(input, factor)
      val ratio = out.size.toDouble() / input.size
      assertTrue(
        "factor=$factor produced length ratio $ratio, expected near 1.0",
        abs(ratio - 1.0) < 0.15,
      )
    }
  }

  /**
   * Every shipped preset must shift by exactly its declared factor. Uses pure tones because a
   * single spectral peak cannot be misread the way a harmonic-rich voice can.
   */
  @Test
  fun `every preset shifts by its declared factor`() {
    val failures = mutableListOf<String>()
    for (effect in VoiceEffect.entries) {
      val inputHz = 240.0
      val out = runShifter(sine(inputHz, 2.0), effect.pitchFactor)
      val measured = estimateFrequency(out)
      val expected = inputHz * effect.pitchFactor
      val errorPct = abs(measured - expected) / expected * 100
      if (errorPct > 5.0) {
        val m = (measured * 10).toInt() / 10.0
        val e = (expected * 10).toInt() / 10.0
        val p = (errorPct * 10).toInt() / 10.0
        failures += effect.name + ": expected " + e + "Hz, measured " + m + "Hz (" + p + "% off)"
      }
    }
    assertTrue("preset shift errors -> " + failures.joinToString("; "), failures.isEmpty())
  }

  @Test
  fun `factors are clamped to the supported range`() {
    val shifter = PitchShifter(sampleRate, 99f)
    assertEquals(PitchShifter.MAX_FACTOR, shifter.pitchFactor, 0.001f)
    shifter.pitchFactor = 0.01f
    assertEquals(PitchShifter.MIN_FACTOR, shifter.pitchFactor, 0.001f)
  }

  @Test
  fun `streaming in odd block sizes matches a single large block`() {
    val input = sine(220.0, 1.0)
    val single = runShifter(input, 1.5f)

    val shifter = PitchShifter(sampleRate, 1.5f)
    val out = FloatArrayBuilder()
    var i = 0
    var block = 97 // deliberately not a divisor of the frame size
    while (i < input.size) {
      val end = minOf(i + block, input.size)
      for (s in shifter.process(input.copyOfRange(i, end))) out.append(s / 32768f)
      i = end
      block = if (block == 97) 313 else 97
    }
    val chunked = out.toShortArray()

    // Block boundaries must not change the result: the engine buffers across calls.
    assertTrue(
      "single=${single.size} chunked=${chunked.size}",
      abs(single.size - chunked.size) < 512,
    )
    assertEquals(estimateFrequency(single), estimateFrequency(chunked), 10.0)
  }

  @Test
  fun `does not clip or emit silence for a loud input`() {
    val out = runShifter(sine(220.0, 1.0), 1.4f)
    assertTrue("engine emitted nothing", out.size > sampleRate / 2)
    val peak = out.maxOf { abs(it.toInt()) }
    assertTrue("output is silent (peak=$peak)", peak > 5000)
    assertTrue("output is clipping (peak=$peak)", peak < 32767)
  }
}
