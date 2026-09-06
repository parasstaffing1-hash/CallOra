package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Callora", appName)
  }

  @Test
  fun `whatsapp recording platform is correctly configured`() {
    val recording = com.example.data.model.CallRecording(
      id = 1L,
      title = "Agency Discovery Call",
      clientName = "Alex Rivera",
      clientCompany = "Nexus Brands",
      clientPhone = "+15551234567",
      callType = com.example.data.model.CallType.DISCOVERY,
      direction = com.example.data.model.CallDirection.OUTBOUND,
      timestamp = System.currentTimeMillis(),
      durationMs = 180000L,
      audioFilePath = "/storage/emulated/0/Callora/call_1.m4a",
      fileSize = 2048000L,
      platform = "WhatsApp",
      audioSourceUsed = "VOICE_COMMUNICATION"
    )

    assertEquals("WhatsApp", recording.platform)
    assertEquals("VOICE_COMMUNICATION", recording.audioSourceUsed)
  }
}
