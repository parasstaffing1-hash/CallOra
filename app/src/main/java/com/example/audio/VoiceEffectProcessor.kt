package com.example.audio

import java.io.File

/**
 * Applies a [VoiceEffect] to already-captured audio.
 *
 * This is the offline half of the voice changer: it works today, with no VoIP stack, because it
 * operates on a finished buffer rather than a live call. [VoiceChangerEngine] is the live half.
 */
object VoiceEffectProcessor {

  /** Streaming block size. Matches the live engine so both paths hit the same WSOLA behaviour. */
  private const val BLOCK = 1024

  /**
   * Applies [effect] to mono 16-bit PCM.
   *
   * Feeding the shifter in blocks rather than one huge array keeps peak memory flat and makes
   * the offline result identical to what the live engine produces.
   */
  fun process(pcm: ShortArray, sampleRate: Int, effect: VoiceEffect): ShortArray {
    if (effect == VoiceEffect.NONE || pcm.isEmpty()) return pcm.copyOf()

    val shifter = PitchShifter(sampleRate, effect.pitchFactor)
    val out = ShortArrayBuilder(pcm.size)
    var i = 0
    while (i < pcm.size) {
      val end = minOf(i + BLOCK, pcm.size)
      out.append(shifter.process(pcm.copyOfRange(i, end)))
      i = end
    }
    return out.toShortArray()
  }

  /**
   * Applies [effect] to a mono WAV file.
   *
   * Multi-channel input is rejected rather than silently mangled: interleaved frames would need
   * per-channel shifting, and Callora records mono.
   */
  fun processWav(input: File, output: File, effect: VoiceEffect) {
    val audio = WavIo.read(input)
    require(audio.channels == 1) { "Only mono WAV is supported, got ${audio.channels} channels" }
    val shifted = process(audio.samples, audio.sampleRate, effect)
    WavIo.write(output, WavIo.Audio(shifted, audio.sampleRate, 1))
  }
}

/** Growable 16-bit accumulator, so the offline path does not reallocate per block. */
internal class ShortArrayBuilder(initialCapacity: Int = 4096) {
  private var data = ShortArray(maxOf(initialCapacity, 1024))
  private var size = 0

  fun append(values: ShortArray) {
    if (size + values.size > data.size) {
      var cap = data.size
      while (cap < size + values.size) cap *= 2
      data = data.copyOf(cap)
    }
    System.arraycopy(values, 0, data, size, values.size)
    size += values.size
  }

  fun toShortArray(): ShortArray = data.copyOf(size)
}
