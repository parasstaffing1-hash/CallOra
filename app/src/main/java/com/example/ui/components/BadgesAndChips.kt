package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallDirection
import com.example.data.model.CallType
import com.example.data.model.Sentiment
import com.example.ui.theme.*

@Composable
fun SentimentBadge(sentiment: Sentiment, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (sentiment) {
        Sentiment.HIGH_INTENT -> Triple(SleekPrimaryContainer, SleekOnPrimaryContainer, "High Intent 🚀")
        Sentiment.POSITIVE -> Triple(SleekEmerald.copy(alpha = 0.12f), SleekEmerald, "Positive 😊")
        Sentiment.NEUTRAL -> Triple(SleekSurfaceVariant, SleekTextSecondary, "Neutral ⚖️")
        Sentiment.HESITANT -> Triple(SleekAmber.copy(alpha = 0.12f), SleekAmber, "Hesitant ⚠️")
        Sentiment.AT_RISK -> Triple(SleekRose.copy(alpha = 0.12f), SleekRose, "At Risk 🚨")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CallTypeChip(callType: CallType, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (callType) {
        CallType.DISCOVERY -> Pair(SleekPrimaryContainer, SleekOnPrimaryContainer)
        CallType.CLIENT_PITCH -> Pair(SleekCyanContainer, SleekCyan)
        CallType.STATUS_UPDATE -> Pair(SleekSurfaceVariant, SleekTextSecondary)
        CallType.NEGOTIATION -> Pair(SleekAmber.copy(alpha = 0.12f), SleekAmber)
        CallType.CLIENT_SUPPORT -> Pair(SleekSurfaceVariant, SleekTextSecondary)
        CallType.INTERNAL_STRATEGY -> Pair(SleekPrimaryContainer, SleekOnPrimaryContainer)
        CallType.CONSULTING -> Pair(SleekCyanContainer, SleekCyan)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = callType.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CallDirectionIcon(direction: CallDirection, modifier: Modifier = Modifier) {
    when (direction) {
        CallDirection.INBOUND -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Inbound",
                    tint = SleekEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Text("Inbound", fontSize = 11.5.sp, color = SleekEmerald, fontWeight = FontWeight.Medium)
            }
        }
        CallDirection.OUTBOUND -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Outbound",
                    tint = SleekPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Text("Outbound", fontSize = 11.5.sp, color = SleekPrimary, fontWeight = FontWeight.Medium)
            }
        }
        CallDirection.MEETING -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Meeting",
                    tint = SleekTextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text("Meeting", fontSize = 11.5.sp, color = SleekTextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ClientAvatar(
    name: String,
    company: String = "",
    colorHex: String = "#0061A4",
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { if (company.isNotBlank()) company.take(2).uppercase() else "AC" }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(SleekSurfaceVariant)
            .border(1.dp, SleekBorder.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size * 0.36).sp,
            fontWeight = FontWeight.Bold,
            color = SleekTextSecondary
        )
    }
}
