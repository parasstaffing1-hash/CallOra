package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecording
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalloraCrmExportDialog(
    call: CallRecording,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val crmStates by viewModel.crmSyncStates.collectAsState()
    var isSyncing by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val recapText = remember(call) {
        viewModel.generateCalloraRecap(call)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalloraGlyph(size = 28.dp)
                Column {
                    Text(
                        text = "Callora CRM & Deliverables",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )
                    Text(
                        text = "Automated deal pipeline sync & executive recap",
                        fontSize = 11.5.sp,
                        color = AgencyTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // CRM Direct Webhook Sync Buttons
                Text(
                    text = "One-Click CRM & Webhook Sync",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgencyTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // HubSpot
                    CrmPlatformButton(
                        name = "HubSpot",
                        color = Color(0xFFFF7A59),
                        isSynced = crmStates["HubSpot"] == true,
                        isLoading = isSyncing == "HubSpot",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isSyncing = "HubSpot"
                            viewModel.syncToCrm("HubSpot", call) { success, msg ->
                                isSyncing = null
                                statusMessage = msg
                            }
                        }
                    )

                    // Salesforce
                    CrmPlatformButton(
                        name = "Salesforce",
                        color = Color(0xFF00A1E0),
                        isSynced = crmStates["Salesforce"] == true,
                        isLoading = isSyncing == "Salesforce",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isSyncing = "Salesforce"
                            viewModel.syncToCrm("Salesforce", call) { success, msg ->
                                isSyncing = null
                                statusMessage = msg
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Notion
                    CrmPlatformButton(
                        name = "Notion",
                        color = Color(0xFF37352F),
                        isSynced = crmStates["Notion"] == true,
                        isLoading = isSyncing == "Notion",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isSyncing = "Notion"
                            viewModel.syncToCrm("Notion", call) { success, msg ->
                                isSyncing = null
                                statusMessage = msg
                            }
                        }
                    )

                    // Slack
                    CrmPlatformButton(
                        name = "Slack #deals",
                        color = Color(0xFF4A154B),
                        isSynced = crmStates["Slack"] == true,
                        isLoading = isSyncing == "Slack",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isSyncing = "Slack"
                            viewModel.syncToCrm("Slack", call) { success, msg ->
                                isSyncing = null
                                statusMessage = msg
                            }
                        }
                    )
                }

                statusMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AgencyEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgencyEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "✓ $msg",
                            fontSize = 11.sp,
                            color = AgencyEmerald,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Divider(color = AgencyBorder)

                // Client Deliverables Brief Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Client Executive Deliverables Brief",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgencyTextPrimary
                    )

                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(recapText))
                            copiedToClipboard = true
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (copiedToClipboard) "Copied!" else "Copy Text",
                            fontSize = 11.5.sp,
                            color = CalloraBlueLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AgencyNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AgencyBorder)
                ) {
                    Text(
                        text = recapText,
                        fontSize = 11.sp,
                        color = AgencyTextSecondary,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.exportCalloraRecap(context, call) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalloraBluePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Branded Brief", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AgencyTextSecondary)
            }
        }
    )
}

@Composable
private fun CrmPlatformButton(
    name: String,
    color: Color,
    isSynced: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading && !isSynced) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSynced) AgencyEmerald.copy(alpha = 0.15f) else AgencyNavyDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSynced) AgencyEmerald else AgencyBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSynced) AgencyEmerald else color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSynced) "$name Synced ✓" else if (isLoading) "Syncing..." else "Sync to $name",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSynced) AgencyEmerald else AgencyTextPrimary
            )
        }
    }
}
