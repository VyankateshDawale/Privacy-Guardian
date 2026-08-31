package com.privacyguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.ui.theme.*

@Composable
fun RiskBadge(level: RiskLevel) {
    val (color, text) = when (level) {
        RiskLevel.CRITICAL -> Critical to "CRITICAL"
        RiskLevel.HIGH -> HighRisk to "HIGH"
        RiskLevel.MEDIUM -> MediumRisk to "MEDIUM"
        RiskLevel.LOW -> Safe to "LOW"
        RiskLevel.SAFE -> Safe to "SAFE"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrivacyStatusCard(riskScore: Int) {
    val isSafe = riskScore < 35
    val statusColor = if (isSafe) Safe else if (riskScore < 70) MediumRisk else Critical
    val statusText = if (isSafe) "PROTECTED" else if (riskScore < 70) "REVIEW NEEDED" else "AT RISK"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Card)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSafe) Icons.Default.VerifiedUser else Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Privacy Risk  $riskScore / 100", color = TextSecondary, fontSize = 12.sp)
            }
            // Circular progress
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                CircularProgressIndicator(
                    progress = { riskScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = statusColor,
                    trackColor = Border,
                    strokeWidth = 4.dp
                )
                Text("$riskScore", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (subtitle != null) {
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color = Safe
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun GuardianModeSelector(selected: com.privacyguardian.domain.model.GuardianMode, onSelect: (com.privacyguardian.domain.model.GuardianMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        com.privacyguardian.domain.model.GuardianMode.values().forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Safe else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode.name,
                    color = if (isSelected) Color.Black else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ExposureCard(score: Int, level: RiskLevel, critical: Int, high: Int, medium: Int, low: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Card)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PRIVACY EXPOSURE", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", color = when (level) {
                    RiskLevel.CRITICAL -> Critical
                    RiskLevel.HIGH -> HighRisk
                    RiskLevel.MEDIUM -> MediumRisk
                    else -> Safe
                }, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(" / 100", color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                Spacer(modifier = Modifier.weight(1f))
                RiskBadge(level)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (critical > 0) Pill("$critical Critical", Critical)
                if (high > 0) Pill("$high High", HighRisk)
                if (medium > 0) Pill("$medium Medium", MediumRisk)
                if (low > 0) Pill("$low Low", Safe)
            }
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun OnDeviceBadge(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Card)
            .border(1.dp, Safe.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Safe, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("PROCESSED ON DEVICE", color = Safe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LoadingState(text: String = "Analyzing…") {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Safe)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Critical, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Critical, fontSize = 14.sp)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)) {
                Text("Try Again")
            }
        }
    }
}
