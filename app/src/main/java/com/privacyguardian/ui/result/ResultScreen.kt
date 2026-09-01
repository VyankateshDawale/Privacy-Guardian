package com.privacyguardian.ui.result

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val level = risk?.riskLevel ?: com.privacyguardian.domain.model.RiskLevel.SAFE

    LaunchedEffect(state.protectedUri) {
        if (state.protectedUri != null) showProtectedComparison = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Scan Complete", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    IconButton(onClick = { showOnDevice = true }) { Icon(Icons.Default.Lock, contentDescription = null, tint = Safe) }
                }
            )
        },
        containerColor = Background
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingState(state.stage)
            }
            return@Scaffold
        }
        if (state.ocrResult == null && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    ErrorState("No scan yet. Go to Scan to pick an image or try Live Demo.", onRetry = onBack)
                }
            }
            return@Scaffold
        }

        // Handle no text
        if (state.ocrResult?.fullText?.isBlank() == true) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No readable text detected.", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Try another image with clearer text.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)) {
                        Text("BACK TO SCAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Scaffold
        }

        // Check zero entities
        if (entities.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    ExposureCard(score = 0, level = com.privacyguardian.domain.model.RiskLevel.SAFE, critical = 0, high = 0, medium = 0, low = 0)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Safe, modifier = Modifier.size(48.dp))
                            Text("No sensitive information detected.", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                            Text("Privacy Risk: 0/100", color = Safe, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
                            Text("Safe to share.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                // Show OCR preview
                item {
                    Text("Detected text preview:", color = TextSecondary, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth().background(Card, RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Text(state.ocrResult?.fullText?.take(600) ?: "", color = TextPrimary, fontSize = 13.sp)
                    }
                }
                item {
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CardElevated, contentColor = TextPrimary)) {
                        Text("BACK")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                val critical = entities.count { it.riskLevel == com.privacyguardian.domain.model.RiskLevel.CRITICAL }
                val high = entities.count { it.riskLevel == com.privacyguardian.domain.model.RiskLevel.HIGH }
                val medium = entities.count { it.riskLevel == com.privacyguardian.domain.model.RiskLevel.MEDIUM }
                val low = entities.count { it.riskLevel == com.privacyguardian.domain.model.RiskLevel.LOW }
                ExposureCard(score, level, critical, high, medium, low)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OnDeviceBadge(onClick = { showOnDevice = true })
                }
            }
            // Risk explanation
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(risk?.explanation ?: "", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            // Image preview with bounding boxes
            if (state.originalBitmap != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .onSizeChanged { imageContainerSize = it }
                            ) {
                                val bmp = if (showProtectedComparison && state.protectedBitmap != null) state.protectedBitmap else state.originalBitmap
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Scanned image",
                                        modifier = Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height.toFloat()),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Overlay only on original
                                    if (!showProtectedComparison) {
                                        val boxes = entities.mapNotNull { e ->
                                            e.boundingBox?.let { BoxOverlay(it, e.riskLevel) }
                                        }
                                        // Compute display rect mapping; need to approximate container dims
                                        // We'll use imageContainerSize
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
                                    // Label
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (showProtectedComparison) Safe else Critical)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .align(Alignment.TopStart)
                                    ) {
                                        Text(if (showProtectedComparison) "PROTECTED" else "ORIGINAL", color = if (showProtectedComparison) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (state.protectedBitmap != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !showProtectedComparison,
                                        onClick = { showProtectedComparison = false },
                                        label = { Text("ORIGINAL", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Safe, selectedLabelColor = Color.Black)
                                    )
                                    FilterChip(
                                        selected = showProtectedComparison,
                                        onClick = { showProtectedComparison = true },
                                        label = { Text("PROTECTED", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Safe, selectedLabelColor = Color.Black)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (showProtectedComparison) {
                                        Text("Processed on device", color = Safe, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (state.originalBitmap == null && state.ocrResult != null) {
                // Text-only result - show masked text
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Sanitized Text Preview", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val sanitized = scanViewModel.getSanitizedText()
                            Box(modifier = Modifier.fillMaxWidth().background(CardElevated, RoundedCornerShape(8.dp)).padding(10.dp)) {
                                Text(sanitized, color = TextPrimary, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("sanitized", sanitized))
                                    Toast.makeText(context, "Copied Safe Version", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)
                            ) { Text("COPY MASKED TEXT", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            // Detected items
            item { SectionHeader("Detected Items", subtitle = "${entities.size} sensitive element(s) found") }
            items(entities) { entity ->
                val isExpanded = expandedId == entity.id
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { expandedId = if (isExpanded) null else entity.id },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Card)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(when(entity.riskLevel){
                                com.privacyguardian.domain.model.RiskLevel.CRITICAL -> Critical
                                com.privacyguardian.domain.model.RiskLevel.HIGH -> HighRisk
                                com.privacyguardian.domain.model.RiskLevel.MEDIUM -> MediumRisk
                                else -> Safe
                            }))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entity.type.name.replace("_"," "), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            RiskBadge(entity.riskLevel)
                            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(entity.maskedValue, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Confidence: ${(entity.confidence*100).toInt()}% • ${entity.context}", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Border, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("WHY IS THIS RISKY?", color = Warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(entity.explanation, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("WHAT CAN LEAK?", color = Critical, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(com.privacyguardian.detection.DetectionPatterns.whatCanLeakFor(entity.type) + " • ${entity.recommendedAction}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // What can leak summary
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("WHAT CAN LEAK?", color = Critical, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        entities.forEach { e ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(e.type.name.replace("_"," "), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("→", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                Text(com.privacyguardian.detection.DetectionPatterns.whatCanLeakFor(e.type), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                            if (e != entities.last()) HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Text("All exposures are potential — cautious language: may, could.", color = TextTertiary, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            // Smart Mask vs Full Redaction toggle (P2 nice to have — now functional)
            item {
                if (state.protectedBitmap == null) {
                    var smartMask by remember { mutableStateOf(false) }
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
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Safe, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = smartMask,
                            onClick = { smartMask = true },
                            label = { Text("SMART MASK", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentBlue, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (smartMask) "Smart Mask keeps structure (j***@iqoo.com) while hiding dangerous content." else "Full Redaction blackouts entire sensitive region — most secure for demo.",
                        color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = { scanViewModel.protectCurrent(context, smartMask) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (smartMask) "SMART PROTECT & SAVE" else "PROTECT & SAVE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            // Mark as ignored
                            Toast.makeText(context, "Ignored", Toast.LENGTH_SHORT).show()
                            onBackToHome()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("IGNORE")
                    }
                } else {
                    // After protection
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Safe.copy(alpha = 0.12f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Safe)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("SAFE COPY CREATED", color = Safe, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("🔒 PROTECTED • Processed on device", color = TextSecondary, fontSize = 11.sp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHARE SAFE COPY", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("DONE") }
                }
            }
        }
    }

    if (showOnDevice) {
        ModalBottomSheet(onDismissRequest = { showOnDevice = false }) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 30.dp)) {
                Text("🔒 PROCESSED ON DEVICE", color = Safe, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Privacy Guardian processes supported content locally. Sensitive image and OCR content is not uploaded to a cloud service by this prototype.", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}



