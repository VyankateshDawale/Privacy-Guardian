package com.privacyguardian.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.ui.theme.*

@Composable
fun RiskBadge(level: RiskLevel) {
    val (color, bg, text) = when (level) {
        RiskLevel.CRITICAL -> Triple(Critical, CriticalLight, "CRITICAL")
        RiskLevel.HIGH -> Triple(HighRisk, CriticalLight, "HIGH")
        RiskLevel.MEDIUM -> Triple(MediumRisk, Color(0xFFFFF7ED), "MEDIUM")
        RiskLevel.LOW -> Triple(Safe, SafeLight, "LOW")
        RiskLevel.SAFE -> Triple(Safe, SafeLight, "SAFE")
    }
    // Pulse animation for critical
    val pulse by rememberInfiniteTransition(label = "riskPulse").animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val scale = if (level == RiskLevel.CRITICAL) pulse else 1f
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
fun PrivacyStatusCard(riskScore: Int) {
    val isSafe = riskScore < 35
    val statusColor = if (isSafe) Safe else if (riskScore < 70) MediumRisk else Critical
    val statusText = if (isSafe) "PROTECTED" else if (riskScore < 70) "REVIEW NEEDED" else "AT RISK"
    val statusIcon = if (isSafe) Icons.Default.VerifiedUser else Icons.Default.Shield

    // Shimmer infinite for border
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "shimmer"
    )
    val brush = Brush.linearGradient(
        colors = listOf(statusColor.copy(alpha = 0.0f), statusColor.copy(alpha = 0.18f), statusColor.copy(alpha = 0.0f)),
        start = Offset(shimmer * 800 - 400, 0f), end = Offset(shimmer * 800 + 400, 100f)
    )

    // Score scale animation
    var targetScore by remember { mutableStateOf(0) }
    LaunchedEffect(riskScore) { targetScore = riskScore }
    val animScore by animateIntAsState(targetValue = targetScore, animationSpec = tween(900, easing = FastOutSlowInEasing), label = "score")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .border(1.dp, Border, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.background(brush).padding(1.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(19.dp),
                colors = CardDefaults.cardColors(containerColor = Card)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated icon container with pulse
                    val pulseScale by rememberInfiniteTransition(label = "iconPulse").animateFloat(
                        initialValue = 1f, targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "icon"
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(if (isSafe) pulseScale else 1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(statusText, color = statusColor, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.8.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Privacy Guardian active", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Privacy Risk  $animScore / 100", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        CircularProgressIndicator(
                            progress = { animScore / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = statusColor,
                            trackColor = Border,
                            strokeWidth = 5.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$animScore", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("/100", color = TextTertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Safe))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = (-0.2).sp)
        }
        if (subtitle != null) {
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 11.dp, top = 2.dp))
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
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120, easing = FastOutSlowInEasing), label = "cardScale")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(16.dp), clip = false)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.06f))
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = TextSecondary, fontSize = 11.5.sp, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun GuardianModeSelector(selected: com.privacyguardian.domain.model.GuardianMode, onSelect: (com.privacyguardian.domain.model.GuardianMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        com.privacyguardian.domain.model.GuardianMode.values().forEach { mode ->
            val isSelected = mode == selected
            val bg = if (isSelected) Brush.linearGradient(listOf(Safe, AccentIqoo)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            val scale by animateFloatAsState(if (isSelected) 1f else 0.95f, tween(200), label = "mode")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Safe else Color.Transparent)
                    .then(if (isSelected) Modifier.background(bg) else Modifier)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode.name,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ExposureCard(score: Int, level: RiskLevel, critical: Int, high: Int, medium: Int, low: Int) {
    val levelColor = when (level) {
        RiskLevel.CRITICAL -> Critical
        RiskLevel.HIGH -> HighRisk
        RiskLevel.MEDIUM -> MediumRisk
        else -> Safe
    }
    // Animated score
    var animTarget by remember { mutableStateOf(0) }
    LaunchedEffect(score) { animTarget = score }
    val animScore by animateIntAsState(animTarget, tween(1000, easing = FastOutSlowInEasing), label = "expScore")

    // Gradient border animation
    val shift by rememberInfiniteTransition(label = "grad").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "shift"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), clip = false)
            .border(1.dp, Border, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Card)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(levelColor.copy(alpha = 0.08f), Color.Transparent, levelColor.copy(alpha = 0.04f)),
                        start = Offset(shift * 600 - 300, 0f), end = Offset(shift * 600 + 300, 300f)
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = levelColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PRIVACY EXPOSURE", color = TextTertiary, fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$animScore", color = levelColor, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 42.sp)
                    Text(" / 100", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    RiskBadge(level)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (critical > 0) Pill("$critical Critical", Critical)
                    if (high > 0) Pill("$high High", HighRisk)
                    if (medium > 0) Pill("$medium Medium", MediumRisk)
                    if (low > 0) Pill("$low Low", Safe)
                    if (critical == 0 && high == 0 && medium == 0 && low == 0) Pill("Clean", Safe)
                }
                // Progress bar with gradient
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Border)
                ) {
                    val progress by animateFloatAsState(score / 100f, tween(1200, easing = FastOutSlowInEasing), label = "prog")
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = when (level) {
                                        RiskLevel.CRITICAL -> listOf(Critical, HighRisk)
                                        RiskLevel.HIGH -> listOf(HighRisk, MediumRisk)
                                        RiskLevel.MEDIUM -> listOf(MediumRisk, Warning)
                                        else -> listOf(Safe, AccentIqoo)
                                    }
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OnDeviceBadge(onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "badgePulse").animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "badge"
    )
    Row(
        modifier = Modifier
            .scale(pulse)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(SafeLight, Color.White))
            )
            .border(1.2.dp, Safe.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Safe)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text("PROCESSED ON DEVICE", color = Safe, fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Safe)) // live dot
    }
}


@Composable
fun LoadingState(text: String = "Analyzing...") {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing)), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing)), label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            // Pulsing radar rings
            Box(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha).border(2.dp, AccentIqoo, CircleShape))
            Box(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale * 0.7f, scaleY = scale * 0.7f, alpha = alpha).background(AccentIqoo.copy(alpha=0.3f), CircleShape))
            
            // Central Icon
            Box(modifier = Modifier.size(48.dp).background(Card, CircleShape).border(2.dp, AccentIqoo, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = AccentIqoo, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("iQOO 15 NPU AI processing...", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceVariant)
                .border(1.dp, Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(subtitle, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp)
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CriticalLight)
                .border(1.dp, Critical.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Critical, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(message, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CrazyLiveDemoCard(onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "demo")
    val shift by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "shift")
    val scale by infinite.animateFloat(1f, 1.02f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    val brush = Brush.linearGradient(
        colors = listOf(DemoGradientStart, DemoGradientMid, DemoGradientEnd, DemoGradientStart),
        start = Offset(shift * 1000 - 500, 0f), end = Offset(shift * 1000 + 500, 400f)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(16.dp, RoundedCornerShape(20.dp), clip = false)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(1.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.White, Color.White.copy(alpha = 0.96f))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush)
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = DemoGradientStart, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ LIVE DEMO", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Critical)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("TAP →", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text("Real OCR + detection in <2s", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("No image needed • Works on any phone", color = TextSecondary, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(brush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
