package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var highBitrateAudio by remember { mutableStateOf(true) }
    var autoAIAnalysis by remember { mutableStateOf(true) }
    var noiseSuppression by remember { mutableStateOf(true) }

    val twoPartyConsent by viewModel.twoPartyConsentPingEnabled.collectAsStateWithLifecycle()
    val piiRedaction by viewModel.piiRedactionEnabled.collectAsStateWithLifecycle()
    val isFloatingWidgetEnabled by viewModel.isFloatingWidgetEnabled.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalloraGlyph(size = 24.dp)
                        Text(
                            text = "Callora Studio Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgencyNavySurface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AgencyNavyDark),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Callora Brand Banner Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CalloraBluePrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CalloraBrandLogo(
                            logoSize = 56.dp,
                            showWordmark = true
                        )

                        Text(
                            text = "The High-Stakes Deal Intelligence OS for Agencies & Consultancies",
                            fontSize = 12.sp,
                            color = AgencyTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Callora Live Intelligence & Coaching Suite
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CalloraBlueLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Live In-Call Intelligence & HUD",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgencyTextPrimary
                            )
                        }

                        SettingToggleRow(
                            title = "In-Call Floating Widget Overlay",
                            subtitle = "Show draggable Callora pill on top of WhatsApp and phone dialers with 1-tap bookmarks and battlecards",
                            checked = isFloatingWidgetEnabled,
                            onCheckedChange = { viewModel.toggleFloatingWidget(it) }
                        )

                        Divider(color = AgencyBorder.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = "Two-Party Consent Audio Ping",
                            subtitle = "Audibly chimes at call start to satisfy legal two-party recording consent requirements",
                            checked = twoPartyConsent,
                            onCheckedChange = {
                                viewModel.twoPartyConsentPingEnabled.value = it
                                if (it) viewModel.playConsentPing(context)
                            }
                        )

                        Divider(color = AgencyBorder.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = "Confidential PII Redaction",
                            subtitle = "Automatically censors credit cards, social security IDs, and sensitive payment data in exported transcripts",
                            checked = piiRedaction,
                            onCheckedChange = { viewModel.piiRedactionEnabled.value = it }
                        )
                    }
                }
            }

            // Audio Engine Quality Settings
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Audio Recording Studio Engine",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )

                        SettingToggleRow(
                            title = "High Bitrate AAC (192 kbps)",
                            subtitle = "Crystal-clear microphone audio capture optimized for vocal clarity",
                            checked = highBitrateAudio,
                            onCheckedChange = { highBitrateAudio = it }
                        )

                        Divider(color = AgencyBorder.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = "Vocal Noise Suppression",
                            subtitle = "Reduces ambient office and keyboard noise during agency calls",
                            checked = noiseSuppression,
                            onCheckedChange = { noiseSuppression = it }
                        )
                    }
                }
            }

            // WhatsApp VoIP Call Recording Engine
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val isNotificationAccessGranted = remember {
                    val enabledListeners = android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        "enabled_notification_listeners"
                    ) ?: ""
                    enabledListeners.contains(context.packageName)
                }
                var whatsAppAutoDetect by remember { mutableStateOf(true) }
                var whatsAppEchoCancellation by remember { mutableStateOf(true) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "WhatsApp & VoIP Call Recording",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgencyTextPrimary
                                )
                                Text(
                                    text = "VOICE_COMMUNICATION hardware engine with Acoustic Echo Cancellation",
                                    fontSize = 11.sp,
                                    color = Color(0xFF25D366)
                                )
                            }
                        }

                        SettingToggleRow(
                            title = "Hardware Echo Cancellation (AEC)",
                            subtitle = "Routes VoIP audio through bidirectional voice communications pipe",
                            checked = whatsAppEchoCancellation,
                            onCheckedChange = { whatsAppEchoCancellation = it }
                        )

                        Divider(color = AgencyBorder.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = "Incoming WhatsApp Call Detection",
                            subtitle = "Alerts to instantly trigger studio recording when a client calls on WhatsApp",
                            checked = whatsAppAutoDetect,
                            onCheckedChange = { whatsAppAutoDetect = it }
                        )

                        if (!isNotificationAccessGranted) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AgencyNavyDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Notification Access Required for Auto-Detect",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AgencyAmber
                                        )
                                        Text(
                                            text = "Enable notification listener in system settings to automatically detect incoming WhatsApp client calls.",
                                            fontSize = 11.sp,
                                            color = AgencyTextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // Fallback to application settings
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF075E54).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "💡 Agency Best Practice for WhatsApp Calls: Android routes remote caller audio through speaker or earpiece. For best two-way audio fidelity on client recordings, turn speakerphone on or use a wired/Bluetooth headset.",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // AI Intelligence Settings
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Gemini 3.5 Flash Intelligence",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgencyTextPrimary
                        )

                        SettingToggleRow(
                            title = "Auto-Analyze on Recording Finish",
                            subtitle = "Instantly synthesize executive takeaways, action items, and draft emails",
                            checked = autoAIAnalysis,
                            onCheckedChange = { autoAIAnalysis = it }
                        )
                    }
                }
            }

            // Legal & Compliance Call Disclaimer
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = AgencyAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Recording Compliance & Consent",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AgencyTextPrimary
                            )
                        }

                        Text(
                            text = "Please ensure compliance with your jurisdiction's one-party or two-party consent laws when recording agency calls with prospective or existing clients.",
                            fontSize = 12.sp,
                            color = AgencyTextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // App Version & Storage
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AgencyNavyCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Callora Studio v2.0 - Agency Intelligence", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AgencyTextPrimary)
                        Text("Built for High-Stakes Agency Pitches, Client Retainers & Deal Execution", fontSize = 11.5.sp, color = AgencyTextSecondary)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = AgencyTextPrimary)
            Text(text = subtitle, fontSize = 11.5.sp, color = AgencyTextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AgencyPrimary,
                uncheckedTrackColor = AgencyBorder
            )
        )
    }
}
