package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionItem
import com.example.ui.theme.*

@Composable
fun ActionItemCard(
    actionItem: ActionItem,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleCompleted() },
        shape = RoundedCornerShape(14.dp),
        color = if (actionItem.isCompleted) SleekSurfaceVariant.copy(alpha = 0.6f) else SleekSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (actionItem.isCompleted) SleekBorderSubtle else SleekBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (actionItem.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "Toggle completed",
                    tint = if (actionItem.isCompleted) SleekEmerald else SleekTextMuted
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionItem.task,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (actionItem.isCompleted) SleekTextMuted else SleekTextPrimary,
                    textDecoration = if (actionItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Assignee badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SleekPrimaryContainer
                    ) {
                        Text(
                            text = "👤 ${actionItem.assignee}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SleekOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    // Due date
                    if (actionItem.dueDate.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SleekAmber.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "⏰ ${actionItem.dueDate}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SleekAmber,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    // Priority
                    if (actionItem.priority.isNotBlank() && actionItem.priority != "Medium") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (actionItem.priority == "High") SleekRose.copy(alpha = 0.12f) else SleekCyanContainer
                        ) {
                            Text(
                                text = actionItem.priority,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (actionItem.priority == "High") SleekRose else SleekCyan,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
