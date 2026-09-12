package com.example.audio

import java.io.File
import java.io.IOException

/**
 * Minimal 16-bit PCM WAV reader/writer.
 *
 * Callora records AAC in an MP4 container, which needs MediaCodec to decode and therefore only
 * works on-device. WAV is the format the offline effect pipeline can also exercise on the JVM,
 * so the same code path is unit-testable off-device.
 */
object WavIo {

  data class Audio(val samples: ShortArray, val sampleRate: Int, val channels: Int) {
    // Data class equals on a ShortArray compares references, which is never what a caller wants.
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Audio) return false
      return sampleRate == other.sampleRate &&
        channels == other.channels &&
        samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int =
      samples.contentHashCode() * 31 * 31 + sampleRate * 31 + channels
  }

  fun read(file: File): Audio = parse(file.readBytes())

  /** Parses a RIFF/WAVE byte stream, tolerating extra chunks before `fmt ` and `data`. */
  fun parse(bytes: ByteArray): Audio {
    if (bytes.size < 44) throw IOException("Too short to be a WAV file")
    if (tag(bytes, 0) != "RIFF" || tag(bytes, 8) != "WAVE") throw IOException("Not a RIFF/WAVE file")

    var channels = 0
    var sampleRate = 0
    var bitsPerSample = 0
    var dataOffset = -1
    var dataLength = 0

    // Chunks are not required to appear in a fixed order, so walk them.
    var p = 12
    while (p + 8 <= bytes.size) {
      val id = tag(bytes, p)
      val size = int32(bytes, p + 4)
      if (size < 0) throw IOException("Corrupt chunk size in '$id'")
      val body = p + 8

      when (id) {
        "fmt " -> {
          if (body + 16 > bytes.size) throw IOException("Truncated fmt chunk")
          val format = int16(bytes, body)
          channels = int16(bytes, body + 2)
          sampleRate = int32(bytes, body + 4)
          bitsPerSample = int16(bytes, body + 14)
          // 1 = PCM, 0xFFFE = WAVE_FORMAT_EXTENSIBLE, which is PCM when bits are 16.
          if (format != 1 && format != 0xFFFE) throw IOException("Unsupported WAV format $format")
        }
        "data" -> {
          dataOffset = body
          dataLength = minOf(size, bytes.size - body)
        }
      }
      // Chunks are word-aligned: an odd size carries a trailing pad byte.
      p = body + size + (size and 1)
    }

    if (dataOffset < 0) throw IOException("No data chunk")
    if (bitsPerSample != 16) throw IOException("Only 16-bit PCM supported, got $bitsPerSample")
    if (channels < 1) throw IOException("Invalid channel count $channels")

    val count = dataLength / 2
    val samples = ShortArray(count)
    for (i in 0 until count) {
      val o = dataOffset + i * 2
      samples[i] = ((bytes[o].toInt() and 0xFF) or (bytes[o + 1].toInt() shl 8)).toShort()
    }
    return Audio(samples, sampleRate, channels)
  }

  fun write(file: File, audio: Audio) {
    file.parentFile?.mkdirs()
    file.writeBytes(encode(audio))
  }

  fun encode(audio: Audio): ByteArray {
    val dataBytes = audio.samples.size * 2
    val out = ByteArray(44 + dataBytes)

    putTag(out, 0, "RIFF")
    putInt32(out, 4, 36 + dataBytes)
    putTag(out, 8, "WAVE")

    putTag(out, 12, "fmt ")
    putInt32(out, 16, 16) // PCM header size
    putInt16(out, 20, 1) // PCM
    putInt16(out, 22, audio.channels)
    putInt32(out, 24, audio.sampleRate)
    putInt32(out, 28, audio.sampleRate * audio.channels * 2) // byte rate
    putInt16(out, 32, audio.channels * 2) // block align
    putInt16(out, 34, 16) // bits per sample

    putTag(out, 36, "data")
    putInt32(out, 40, dataBytes)

    for (i in audio.samples.indices) {
      val v = audio.samples[i].toInt()
      out[44 + i * 2] = (v and 0xFF).toByte()
      out[45 + i * 2] = ((v shr 8) and 0xFF).toByte()
    }
    return out
  }

  private fun tag(b: ByteArray, o: Int) = String(b, o, 4, Charsets.US_ASCII)
  private fun int16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
  private fun int32(b: ByteArray, o: Int) =
    (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
      ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

  private fun putTag(b: ByteArray, o: Int, s: String) {
    for (i in 0 until 4) b[o + i] = s[i].code.toByte()
  }

  private fun putInt16(b: ByteArray, o: Int, v: Int) {
    b[o] = (v and 0xFF).toByte(); b[o + 1] = ((v shr 8) and 0xFF).toByte()
  }

  private fun putInt32(b: ByteArray, o: Int, v: Int) {
    b[o] = (v and 0xFF).toByte(); b[o + 1] = ((v shr 8) and 0xFF).toByte()
    b[o + 2] = ((v shr 16) and 0xFF).toByte(); b[o + 3] = ((v shr 24) and 0xFF).toByte()
  }
}
