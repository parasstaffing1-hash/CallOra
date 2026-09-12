package com.example.audio

import android.content.Context

/**
 * Persists the user's voice-changer preset across process death.
 *
 * Uses SharedPreferences rather than DataStore because the DataStore dependency is currently
 * commented out in the app's build file; swapping the implementation later does not change
 * this API.
 */
class VoiceChangerPreferences(context: Context) {

  private val prefs =
    context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

  var effect: VoiceEffect
    get() {
      val stored = prefs.getString(KEY_EFFECT, null) ?: return VoiceEffect.NONE
      // Stored by enum name so renaming a label cannot break a user's saved preset.
      return runCatching { VoiceEffect.valueOf(stored) }.getOrDefault(VoiceEffect.NONE)
    }
    set(value) = prefs.edit().putString(KEY_EFFECT, value.name).apply()

  private companion object {
    const val FILE_NAME = "callora_voice_changer"
    const val KEY_EFFECT = "effect"
  }
}
