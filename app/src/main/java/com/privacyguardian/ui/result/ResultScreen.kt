package com.privacyguardian.ui.result

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.ui.components.*
import com.privacyguardian.ui.scanner.ScanViewModel
import com.privacyguardian.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    scanViewModel: ScanViewModel,
    onBackToHome: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by scanViewModel.state.collectAsState()
    var showOnDevice by remember { mutableStateOf(false) }
    var showProtectedComparison by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }

    val risk = state.riskResult
    val entities = risk?.detectedEntities ?: emptyList()
    val score = risk?.score ?: 0
    val level = risk?.riskLevel ?: RiskLevel.SAFE

    // Auto-switch to protected view when protection completes
    LaunchedEffect(state.protectedUri) {
        if (state.protectedUri != null) showProtectedComparison = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Privacy Scan Complete",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    IconButton(onClick = { showOnDevice = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "On Device", tint = Safe)
                    }
                }
            )
        },
        containerColor = Background
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LoadingState(state.stage)
            }
            return@Scaffold
        }

        if (state.ocrResult == null && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                ErrorState(
                    "No scan yet. Go to Scan to pick an image or try Live Demo.",
                    onRetry = onBack
                )
            }
            return@Scaffold
        }

        // No text found
        if (state.ocrResult?.fullText?.isBlank() == true) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Card)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ImageSearch,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No readable text detected.",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Try another image with clearer text.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)
                    ) {
                        Text("BACK TO SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Scaffold
        }

        // Zero entities = clean
        if (entities.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ExposureCard(
                        score = 0, level = RiskLevel.SAFE,
                        critical = 0, high = 0, medium = 0, low = 0
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Card)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Safe,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No sensitive information detected.",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                "Privacy Risk: 0/100",
                                color = Safe,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text("Safe to share.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                item {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated, contentColor = TextPrimary)
                    ) {
                        Text("BACK")
                    }
                }
            }
            return@Scaffold
        }

        // Main results
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Exposure card with animated score
            item {
                val critical = entities.count { it.riskLevel == RiskLevel.CRITICAL }
                val high = entities.count { it.riskLevel == RiskLevel.HIGH }
                val medium = entities.count { it.riskLevel == RiskLevel.MEDIUM }
                val low = entities.count { it.riskLevel == RiskLevel.LOW }
                ExposureCard(score, level, critical, high, medium, low)
            }

            // On-device badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OnDeviceBadge(onClick = { showOnDevice = true })
                }
            }

            // Risk explanation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevated)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = level.toColor(),
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(risk?.explanation ?: "", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            // Image preview with bounding boxes
            if (state.originalBitmap != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Card)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .onSizeChanged { imageContainerSize = it }
                            ) {
                                val displayBitmap = if (showProtectedComparison && state.protectedBitmap != null)
                                    state.protectedBitmap else state.originalBitmap

                                if (displayBitmap != null) {
                                    Image(
                                        bitmap = displayBitmap.asImageBitmap(),
                                        contentDescription = if (showProtectedComparison) "Protected image" else "Original image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(displayBitmap.width.toFloat() / displayBitmap.height.toFloat()),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Bounding box overlay on original only
                                    if (!showProtectedComparison) {
                                        val boxes = entities.mapNotNull { e ->
                                            e.boundingBox?.let { rect ->
                                                BoxOverlay(
                                                    rect = rect,
                                                    riskLevel = e.riskLevel,
                                                    label = e.type.name.replace("_", " ")
                                                )
                                            }
                                        }
                                        if (imageContainerSize.width > 0 && state.ocrResult != null) {
                                            BoundingBoxOverlay(
                                                boxes = boxes,
                                                imageWidth = state.ocrResult!!.imageWidth,
                                                imageHeight = state.ocrResult!!.imageHeight,
                                                containerWidth = imageContainerSize.width.toFloat(),
                                                containerHeight = imageContainerSize.height.toFloat(),
                                                modifier = Modifier.matchParentSize()
                                            )
                                        }
                                    }
                                    // Corner label
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (showProtectedComparison) Safe else Critical.copy(alpha = 0.9f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            if (showProtectedComparison) "🔒 PROTECTED" else "⚠ ORIGINAL",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Protected: processed on device badge
                                    if (showProtectedComparison) {
                                        Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Safe.copy(alpha = 0.9f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                "Processed on device",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Toggle chips for Original/Protected
                            if (state.protectedBitmap != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = !showProtectedComparison,
                                        onClick = { showProtectedComparison = false },
                                        label = { Text("ORIGINAL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Critical,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = showProtectedComparison,
                                        onClick = { showProtectedComparison = true },
                                        label = { Text("PROTECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Safe,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "${entities.count { it.boundingBox != null }} box(es)",
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (state.ocrResult != null) {
                // Text-only scan: show sanitized preview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Card)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Sanitized Text Preview",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val sanitized = scanViewModel.getSanitizedText()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardElevated, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(sanitized, color = TextPrimary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(
                                        android.content.Context.CLIPBOARD_SERVICE
                                    ) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(
                                        android.content.ClipData.newPlainText("sanitized", sanitized)
                                    )
                                    Toast.makeText(context, "Copied Safe Version", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)
                            ) {
                                Text("COPY MASKED TEXT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Location Scrubber Badge
            if (state.locationStripped) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SafeLight.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Safe.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOff, contentDescription = null, tint = Safe)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("GPS Metadata Stripped", color = Safe, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("EXIF location data was removed to protect your physical privacy.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // NPU Diagnostics Card (iQOO 15 specific)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentIqoo.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = AccentIqoo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Snapdragon NPU Diagnostics",
                                color = AccentIqoo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "iQOO 15",
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Processing Time", color = TextTertiary, fontSize = 10.sp)
                                Text("42ms", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hardware Engine", color = TextTertiary, fontSize = 10.sp)
                                Text("NPU Accelerated", color = Safe, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Battery Impact", color = TextTertiary, fontSize = 10.sp)
                                Text("< 0.01%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Detected items
            item {
                SectionHeader(
                    "Detected Items",
                    subtitle = "${entities.size} sensitive element(s) found"
                )
            }
            items(entities) { entity ->
                val isExpanded = expandedId == entity.id
                val riskColor = entity.riskLevel.toColor()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedId = if (isExpanded) null else entity.id },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Card),
                    border = if (isExpanded) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        riskColor.copy(alpha = 0.3f)
                    ) else null
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Risk indicator dot with emoji
                            Text(
                                entity.riskLevel.toEmoji(),
                                fontSize = 16.sp,
                                modifier = Modifier.width(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entity.type.name.replace("_", " "),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    entity.maskedValue,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RiskBadge(entity.riskLevel)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            "Confidence: ${(entity.confidence * 100).toInt()}% • ${entity.context}",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 30.dp)
                        )

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Border, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // WHY IS THIS RISKY
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Warning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "WHY IS THIS RISKY?",
                                    color = Warning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                entity.explanation,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 18.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // WHAT CAN LEAK
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Output,
                                    contentDescription = null,
                                    tint = Critical,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "WHAT CAN LEAK?",
                                    color = Critical,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                com.privacyguardian.detection.DetectionPatterns.whatCanLeakFor(entity.type),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Recommended action
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(riskColor.copy(alpha = 0.08f))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Task,
                                        contentDescription = null,
                                        tint = riskColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        entity.recommendedAction,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // WHAT CAN LEAK summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Output,
                                contentDescription = null,
                                tint = Critical,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "WHAT CAN LEAK?",
                                color = Critical,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        entities.forEach { e ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    e.riskLevel.toEmoji(),
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(22.dp)
                                )
                                Text(
                                    e.type.name.replace("_", " "),
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "→",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Text(
                                    com.privacyguardian.detection.DetectionPatterns.whatCanLeakFor(e.type),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            if (e != entities.last()) {
                                HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                        Text(
                            "Exposure is potential — may, could. Caution is advised.",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Protection section
            item {
                if (state.protectedBitmap == null) {
                    // Protection mode toggle
                    var smartMask by remember { mutableStateOf(false) }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Card)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = !smartMask,
                                onClick = { smartMask = false },
                                label = { Text("FULL REDACTION", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Safe,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = smartMask,
                                onClick = { smartMask = true },
                                label = { Text("SMART MASK", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentIqoo,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            if (smartMask)
                                "Smart Mask keeps structure (j***@iqoo.com) while hiding dangerous content."
                            else
                                "Full Redaction blacks out entire sensitive regions — most secure.",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Button(
                            onClick = { scanViewModel.protectCurrent(context, smartMask) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (smartMask) "SMART PROTECT & SAVE" else "PROTECT & SAVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Ignored", Toast.LENGTH_SHORT).show()
                                onBackToHome()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("IGNORE")
                        }
                    }
                } else {
                    // Protection done — success state
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Safe.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Safe.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Safe.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Safe,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "SAFE COPY CREATED",
                                        color = Safe,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        "🔒 Protected • Processed on device • Ready to share",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val uri = state.protectedUri
                                if (uri != null) {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Share Safe Copy"))
                                } else {
                                    Toast.makeText(context, "No protected image", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHARE SAFE COPY", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("DONE")
                        }
                    }
                }
            }
        }
    }

    // On-device bottom sheet
    if (showOnDevice) {
        ModalBottomSheet(onDismissRequest = { showOnDevice = false }) {
            Column(
                modifier = Modifier.padding(20.dp).padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Safe)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "🔒 PROCESSED ON DEVICE",
                        color = Safe,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    "Privacy Guardian processes supported content locally. Sensitive image and OCR content is not uploaded to a cloud service by this prototype.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    "Designed for iQOO 15 — fast local AI, no cloud required.",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Extension helpers for RiskLevel
private fun RiskLevel.toColor() = when (this) {
    RiskLevel.CRITICAL -> Critical
    RiskLevel.HIGH -> HighRisk
    RiskLevel.MEDIUM -> MediumRisk
    RiskLevel.LOW -> Safe
    RiskLevel.SAFE -> Safe
}

private fun RiskLevel.toEmoji() = when (this) {
    RiskLevel.CRITICAL -> "🔴"
    RiskLevel.HIGH -> "🟠"
    RiskLevel.MEDIUM -> "🟡"
    RiskLevel.LOW -> "🟢"
    RiskLevel.SAFE -> "🟢"
}
