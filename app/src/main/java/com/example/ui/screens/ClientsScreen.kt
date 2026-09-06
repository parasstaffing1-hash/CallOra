package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgencyClient
import com.example.data.model.CallType
import com.example.ui.MainViewModel
import com.example.ui.components.ClientAvatar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: MainViewModel,
    onStartCallWithClient: (AgencyClient) -> Unit,
    modifier: Modifier = Modifier
) {
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    var showAddClientDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Clients Directory",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = SleekTextPrimary
                    )
                },
                actions = {
                    Button(
                        onClick = { showAddClientDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Client", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekCanvas)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SleekCanvas),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(clients, key = { it.id }) { client ->
                AgencyClientCard(
                    client = client,
                    onStartCall = { onStartCallWithClient(client) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddClientDialog) {
        AddClientDialog(
            onDismiss = { showAddClientDialog = false },
            onAdd = { newClient ->
                viewModel.addNewClient(newClient)
                showAddClientDialog = false
            }
        )
    }
}

@Composable
fun AgencyClientCard(
    client: AgencyClient,
    onStartCall: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClientAvatar(
                    name = client.name,
                    company = client.company,
                    colorHex = client.avatarColorHex,
                    size = 46
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.company,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "${client.name} • ${client.phone}",
                        fontSize = 12.sp,
                        color = SleekTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SleekPrimaryContainer
                ) {
                    Text(
                        text = client.stage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekOnPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = SleekBorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Value: ${client.monthlyValue}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekEmerald
                    )
                    Text(
                        text = "${client.totalCallsCount} calls logged",
                        fontSize = 12.sp,
                        color = SleekTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (client.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    val uri = android.net.Uri.parse("https://wa.me/${client.phone.replace("+", "").replace(" ", "").replace("-", "")}")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.whatsapp")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://api.whatsapp.com/send?phone=${client.phone}"))
                                    context.startActivity(fallback)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp Client",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onStartCall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimaryContainer,
                            contentColor = SleekOnPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Call", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onAdd: (AgencyClient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("Active Retainer") }
    var monthlyValue by remember { mutableStateOf("$10,000/mo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Agency Client", color = AgencyTextPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Primary Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = monthlyValue,
                    onValueChange = { monthlyValue = it },
                    label = { Text("Monthly Retainer Value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (company.isNotBlank()) {
                        onAdd(
                            AgencyClient(
                                name = name.ifBlank { "Primary Contact" },
                                company = company,
                                phone = phone,
                                email = email,
                                stage = stage,
                                monthlyValue = monthlyValue
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AgencyPrimary)
            ) {
                Text("Add Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AgencyTextSecondary)
            }
        },
        containerColor = AgencyNavySurface
    )
}
