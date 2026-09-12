package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.VoiceEffect
import com.example.ui.theme.*

/**
 * Voice-changer preset picker with local monitoring.
 *
 * The copy deliberately states that the preset applies to Callora's own audio and not to
 * WhatsApp or carrier calls - promising otherwise would be a promise the platform cannot keep.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceChangerCard(
  selected: VoiceEffect,
  isPreviewing: Boolean,
  level: Float,
  error: String?,
  onSelect: (VoiceEffect) -> Unit,
  onTogglePreview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var permissionDenied by remember { mutableStateOf(false) }

  val hasMicPermission = {
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    permissionDenied = !granted
    if (granted) onTogglePreview()
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = AgencyNavyCard,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = null,
          tint = AgencyPrimary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "Voice Changer",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = AgencyTextPrimary,
        )
      }

      Spacer(Modifier.height(4.dp))
      Text(
        text = "Applies to audio Callora records and to calls placed inside Callora.",
        fontSize = 11.5.sp,
        color = AgencyTextSecondary,
      )

      Spacer(Modifier.height(14.dp))

      // Presets. FlowRow keeps the chips readable when labels wrap on narrow screens.
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VoiceEffect.entries.forEach { effect ->
          FilterChip(
            selected = effect == selected,
            onClick = { onSelect(effect) },
            label = { Text(effect.label, fontSize = 12.sp) },
          )
        }
      }

      Spacer(Modifier.height(6.dp))
      Text(
        text = selected.description,
        fontSize = 11.sp,
        color = AgencyTextSecondary,
      )

      Spacer(Modifier.height(14.dp))

      // Live level meter, so the user can see the mic is actually feeding the effect.
      val animatedLevel by animateFloatAsState(
        targetValue = if (isPreviewing) level.coerceIn(0f, 1f) else 0f,
        label = "voice-level",
      )
      LinearProgressIndicator(
        progress = { animatedLevel },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .background(AgencyBorder, RoundedCornerShape(3.dp)),
        color = if (animatedLevel > 0.9f) SleekRose else AgencyPrimary,
        trackColor = Color.Transparent,
      )

      Spacer(Modifier.height(12.dp))

      Button(
        onClick = {
          if (isPreviewing || hasMicPermission()) {
            permissionDenied = false
            onTogglePreview()
          } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.Headset,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(if (isPreviewing) "Stop preview" else "Preview with headphones")
      }

      if (isPreviewing) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = AgencyTextSecondary,
            modifier = Modifier.size(14.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            // On loudspeaker the monitored output re-enters the mic; AEC only partly suppresses it.
            text = "Use headphones. On speaker this will echo or howl.",
            fontSize = 11.sp,
            color = AgencyTextSecondary,
          )
        }
      }

      val message = error ?: if (permissionDenied) {
        "Microphone access is needed to preview your voice."
      } else null

      if (message != null) {
        Spacer(Modifier.height(8.dp))
        Text(text = message, fontSize = 11.5.sp, color = SleekRose)
      }
    }
  }
}
