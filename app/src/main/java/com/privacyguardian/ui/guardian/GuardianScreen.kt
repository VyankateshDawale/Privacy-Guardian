package com.privacyguardian.ui.guardian

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.ui.components.*
import com.privacyguardian.ui.scanner.ScanViewModel
import com.privacyguardian.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(
    app: PrivacyGuardianApp,
    scanViewModel: ScanViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val state by scanViewModel.state.collectAsState()
    var showOnDevice by remember { mutableStateOf(false) }
    var notificationText by remember { mutableStateOf("Your OTP is 482913") }
    var developerText by remember { mutableStateOf("DATABASE_URL=postgres://admin:SuperSecret123@db.example.com:5432/mydb\nAWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE\nAWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\nJWT=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c") }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareCandidateBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            // Simulate share flow
            val bmp = runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(stream)
            }.getOrNull()
            if (bmp != null) {
                shareCandidateBitmap = bmp
                // Run check
                scanViewModel.simulateShareProtection(context, bmp) { hasRisk ->
                    showShareDialog = true
                }
            }
        }
    }

    LaunchedEffect(Unit) { scanViewModel.init(app) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guardian", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = { IconButton(onClick = { showOnDevice = true }) { Icon(Icons.Default.Lock, contentDescription = null, tint = Safe) } }
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Safe)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Guard", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Simulate sharing an image — Privacy Checkpoint will scan before you share.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMULATE SHARE", fontWeight = FontWeight.Bold)
                        }
                        Text("Controlled demonstration — does not intercept system shares.", color = TextTertiary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            // Developer Guard
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = HighRisk)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Developer Guard", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Paste code/config — detect secrets, database URLs, tokens.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = developerText,
                            onValueChange = { developerText = it },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            placeholder = { Text("Paste secrets here...", color = TextTertiary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighRisk,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = HighRisk
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // Analyze inline
                        var localResult by remember { mutableStateOf<com.privacyguardian.domain.model.PrivacyRiskResult?>(null) }
                        LaunchedEffect(developerText) {
                            // quick local detection without viewmodel to avoid image side-effects
                            val entities = app.detector.detect(developerText, emptyList())
                            localResult = app.riskEngine.calculateRisk(entities)
                        }
                        if (localResult != null && (localResult!!.detectedEntities.isNotEmpty())) {
                            val lr = localResult!!
                            Text("${lr.detectedEntities.size} SECRETS DETECTED", color = Critical, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                val crit = lr.detectedEntities.count { it.riskLevel == com.privacyguardian.domain.model.RiskLevel.CRITICAL }
                                val high = lr.detectedEntities.count { it.riskLevel == com.privacyguardian.domain.model.RiskLevel.HIGH }
                                if (crit > 0) RiskBadge(com.privacyguardian.domain.model.RiskLevel.CRITICAL)
                                if (high > 0) RiskBadge(com.privacyguardian.domain.model.RiskLevel.HIGH)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Show masked list
                            lr.detectedEntities.forEach { e ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(e.type.name.replace("_"," "), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(e.maskedValue, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Text("No secrets detected", color = Safe, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val entities = app.detector.detect(developerText, emptyList())
                                var sanitized = developerText
                                entities.forEach { e -> sanitized = sanitized.replace(e.originalValue, e.maskedValue) }
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("sanitized", sanitized))
                                Toast.makeText(context, "Copied Safe Version", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HighRisk, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("REDACT ALL & COPY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Streamer Guard Simulator (iQOO Esports specific)
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentIqoo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Streamer Guard (Esports Shield)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Simulate intercepting notifications during live streams or gaming to prevent data leaks.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = notificationText,
                            onValueChange = { notificationText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentIqoo,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        var notifResult by remember { mutableStateOf<com.privacyguardian.domain.model.PrivacyRiskResult?>(null) }
                        Button(
                            onClick = {
                                val ents = app.detector.detect(notificationText, emptyList())
                                notifResult = app.riskEngine.calculateRisk(ents)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIqoo, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("SIMULATE STREAM NOTIFICATION", fontWeight = FontWeight.Bold) }
                        if (notifResult != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val nr = notifResult!!
                            if (nr.detectedEntities.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("⚠️ Leak Prevented During Game", color = Critical, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        var protectedPreview = notificationText
                                        nr.detectedEntities.forEach { e -> protectedPreview = protectedPreview.replace(e.originalValue, "[REDACTED FOR STREAM]") }
                                        Text("Viewer sees:", color = TextTertiary, fontSize = 11.sp)
                                        Text("\"$protectedPreview\"", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                                        Text("Sensitive parts removed dynamically via NPU before render.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            } else {
                                Text("Safe: No sensitive info detected.", color = Safe, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Guardian Modes Behavior", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Normal: Detect & ask • Strict: Auto-select Critical • Maximum: Auto-redact Critical (never deletes original).", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
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

    if (showShareDialog) {
        val risk = state.riskResult
        val score = risk?.score ?: 0
        val hasRisk = (score >= 35)
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                if (hasRisk) Text("⚠️ SENSITIVE DATA DETECTED", color = Critical, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                else Text("No sensitive data", color = Safe, fontWeight = FontWeight.Bold)
            },
            text = {
                if (hasRisk) Column {
                    Text("Exposure Risk: $score / 100", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(risk?.explanation ?: "", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Detected:", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    risk?.detectedEntities?.take(3)?.forEach { e ->
                        Text("• ${e.type.name.replace("_"," ")}: ${e.maskedValue}", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Text("This image appears safe to share.", color = TextSecondary)
                }
            },
            confirmButton = {
                if (hasRisk) {
                    Button(
                        onClick = {
                            showShareDialog = false
                            // Protect & share
                            if (shareCandidateBitmap != null && risk != null) {
                                scanViewModel.protectCurrent(context)
                                // After protection, share will be triggered via Result screen or directly
                                Toast.makeText(context, "Creating safe copy...", Toast.LENGTH_SHORT).show()
                                // Share protected if available after delay
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)
                    ) { Text("PROTECT & SHARE", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                } else {
                    Button(onClick = { showShareDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.Black)) { Text("SHARE") }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasRisk) {
                        TextButton(onClick = {
                            showShareDialog = false
                            // Share anyway
                            shareCandidateBitmap?.let { bmp ->
                                val engine = app.protectionEngine()
                                // For demo, just share original via share sheet (simulate)
                                val uri = engine.saveBitmapToCache(bmp, "share_anyway.png")
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Share"))
                            }
                        }) { Text("SHARE ANYWAY", color = TextSecondary, fontSize = 12.sp) }
                    }
                    TextButton(onClick = { showShareDialog = false }) { Text("CANCEL") }
                }
            },
            containerColor = Card,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
