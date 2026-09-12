package com.example.audio

/**
 * Voice-changer presets.
 *
 * [pitchFactor] is a frequency multiplier: 1.0 leaves the voice alone, 2.0 is one octave up,
 * 0.5 one octave down. The range is deliberately narrower than the shifter's hard limits
 * because heavy shifting on speech stops sounding like a person.
 */
enum class VoiceEffect(
  val label: String,
  val description: String,
  val pitchFactor: Float,
) {
  NONE("Off", "Your natural voice", 1.0f),
  SUBTLE_DEEP("Subtle Deep", "Slightly lower, still recognisably you", 0.88f),
  DEEP("Deep", "Noticeably lower register", 0.75f),
  BARITONE("Baritone", "Heavy low shift", 0.62f),
  SUBTLE_LIGHT("Subtle Light", "Slightly higher, still natural", 1.12f),
  BRIGHT("Bright", "Noticeably higher register", 1.35f),
  HELIUM("Helium", "Cartoon-high, clearly synthetic", 1.8f),

  /**
   * Shifts far enough that speaker identification is difficult while speech stays
   * intelligible. Intended for the anonymised-clip use case, not for impersonation.
   */
  MASKED("Masked", "Obscures speaker identity, stays intelligible", 0.68f);

  companion object {
    fun fromLabel(label: String): VoiceEffect =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: NONE
  }
}
