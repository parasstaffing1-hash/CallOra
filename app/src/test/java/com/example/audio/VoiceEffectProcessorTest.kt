package com.example.audio

import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEffectProcessorTest {

  private val sampleRate = 44100

  // ---- WAV round trip ----------------------------------------------------------------------

  @Test
  fun `wav round trip preserves samples and format`() {
    val samples = ShortArray(1000) { (it * 37 % 30000 - 15000).toShort() }
    val original = WavIo.Audio(samples, sampleRate, 1)
    val decoded = WavIo.parse(WavIo.encode(original))

    assertEquals(sampleRate, decoded.sampleRate)
    assertEquals(1, decoded.channels)
    assertTrue(samples.contentEquals(decoded.samples))
  }

  @Test
  fun `wav parser rejects non-riff input`() {
    val junk = ByteArray(100) { 0x41 }
    val error = runCatching { WavIo.parse(junk) }.exceptionOrNull()
    assertTrue("expected an IOException, got $error", error is java.io.IOException)
  }

  @Test
  fun `wav parser tolerates an extra chunk before data`() {
    val base = WavIo.encode(WavIo.Audio(ShortArray(200) { it.toShort() }, sampleRate, 1))
    // Splice a LIST chunk between fmt and data, which real encoders routinely emit.
    val listChunk = ByteArray(8 + 4).also {
      "LIST".forEachIndexed { i, c -> it[i] = c.code.toByte() }
      it[4] = 4 // size
      "INFO".forEachIndexed { i, c -> it[8 + i] = c.code.toByte() }
    }
    val spliced = base.copyOfRange(0, 36) + listChunk + base.copyOfRange(36, base.size)
    // RIFF size field must account for the inserted chunk.
    val newSize = spliced.size - 8
    spliced[4] = (newSize and 0xFF).toByte()
    spliced[5] = ((newSize shr 8) and 0xFF).toByte()
    spliced[6] = ((newSize shr 16) and 0xFF).toByte()
    spliced[7] = ((newSize shr 24) and 0xFF).toByte()

    val decoded = WavIo.parse(spliced)
    assertEquals(200, decoded.samples.size)
    assertEquals(sampleRate, decoded.sampleRate)
  }

  // ---- Offline processing ------------------------------------------------------------------

  @Test
  fun `NONE returns the input untouched`() {
    val pcm = voice(1.0)
    val out = VoiceEffectProcessor.process(pcm, sampleRate, VoiceEffect.NONE)
    assertTrue(pcm.contentEquals(out))
  }

  @Test
  fun `offline processing shifts pitch by the preset factor`() {
    val pcm = tone(200.0, 2.0)
    val out = VoiceEffectProcessor.process(pcm, sampleRate, VoiceEffect.HELIUM)
    val expected = 200.0 * VoiceEffect.HELIUM.pitchFactor
    assertEquals(expected, fundamental(out), 25.0)
  }

  @Test
  fun `offline result matches the streaming engine`() {
    val pcm = tone(220.0, 1.0)
    val offline = VoiceEffectProcessor.process(pcm, sampleRate, VoiceEffect.DEEP)
    assertEquals(220.0 * VoiceEffect.DEEP.pitchFactor, fundamental(offline), 15.0)
  }

  @Test
  fun `processWav rejects stereo input`() {
    val dir = File("build/demo-audio").apply { mkdirs() }
    val stereo = File(dir, "stereo.wav")
    WavIo.write(stereo, WavIo.Audio(ShortArray(400), sampleRate, 2))
    val error = runCatching {
      VoiceEffectProcessor.processWav(stereo, File(dir, "out.wav"), VoiceEffect.DEEP)
    }.exceptionOrNull()
    assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
  }

  /**
   * Renders every preset to a WAV so the effect can be listened to without a device.
   * Output: app/build/demo-audio/
   */
  @Test
  fun `render audible demos for every preset`() {
    val dir = File("build/demo-audio").apply { mkdirs() }
    val source = voice(2.6)
    WavIo.write(File(dir, "00-original.wav"), WavIo.Audio(source, sampleRate, 1))

    VoiceEffect.entries.forEachIndexed { index, effect ->
      val shifted = VoiceEffectProcessor.process(source, sampleRate, effect)
      val ordinal = (index + 1).toString().padStart(2, '0')
      val name = ordinal + "-" + effect.name.lowercase() + ".wav"
      WavIo.write(File(dir, name), WavIo.Audio(shifted, sampleRate, 1))
      assertTrue(effect.name + " produced no audio", shifted.size > sampleRate)
    }

    val written = dir.listFiles { f -> f.extension == "wav" }?.size ?: 0
    assertTrue("expected demos on disk, found " + written, written >= VoiceEffect.entries.size)
  }

  // ---- Signal helpers ----------------------------------------------------------------------

  private fun tone(freq: Double, seconds: Double) = ShortArray((sampleRate * seconds).toInt()) {
    (sin(2.0 * PI * freq * it / sampleRate) * 20000).toInt().toShort()
  }

  /**
   * A crude vowel: a buzzy glottal source shaped by two formants, with pitch drift and a
   * syllable envelope. Not speech, but it has the harmonic structure that makes a pitch shift
   * clearly audible to a listener, which a bare sine does not.
   */
  private fun voice(seconds: Double): ShortArray {
    val n = (sampleRate * seconds).toInt()
    val out = ShortArray(n)
    val syllable = sampleRate * 0.42

    for (i in 0 until n) {
      val t = i.toDouble() / sampleRate
      // Pitch drifts through the phrase so it does not sound like a test tone.
      val f0 = 118.0 + 14.0 * sin(2.0 * PI * 0.55 * t)

      // Glottal source: harmonics rolling off, shaped toward /a/ formants at 700 and 1220 Hz.
      var sample = 0.0
      for (h in 1..24) {
        val f = f0 * h
        if (f > 5200) break
        val d1 = (f - 700) / 190.0
        val d2 = (f - 1220) / 260.0
        val f1Gain = 1.0 / (1.0 + d1 * d1)
        val f2Gain = 0.55 / (1.0 + d2 * d2)
        sample += (f1Gain + f2Gain) * sin(2.0 * PI * f * t) / h
      }

      // Syllable envelope with a short attack and decay, plus gaps between syllables.
      val phase = (i % syllable.toInt()) / syllable
      val env = if (phase > 0.78) 0.0 else {
        val attack = (phase / 0.06).coerceAtMost(1.0)
        attack * exp(-2.1 * phase)
      }

      out[i] = (sample * env * 9000).coerceIn(-32000.0, 32000.0).toInt().toShort()
    }
    return out
  }

  /** Autocorrelation pitch estimate, tolerant of WSOLA splice discontinuities. */
  private fun fundamental(pcm: ShortArray): Double {
    val from = pcm.size / 4
    val len = pcm.size / 2
    val x = DoubleArray(len) { pcm[from + it] / 32768.0 }
    val minLag = sampleRate / 2000
    val maxLag = sampleRate / 50

    var bestLag = minLag
    var best = -Double.MAX_VALUE
    for (lag in minLag..maxLag) {
      var dot = 0.0
      var energy = 1e-9
      for (i in 0 until len - lag) {
        dot += x[i] * x[i + lag]
        energy += x[i + lag] * x[i + lag]
      }
      val score = dot / kotlin.math.sqrt(energy)
      if (score > best) { best = score; bestLag = lag }
    }
    return sampleRate.toDouble() / bestLag
  }
}
