package com.privacyguardian.ui.guardian

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.ui.theme.*
import com.privacyguardian.ui.scanner.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(app: PrivacyGuardianApp, scanViewModel: ScanViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var developerText by remember { mutableStateOf("") }
    
    // Simulate share stuff
    var showShareDialog by remember { mutableStateOf(false) }
    var shareCandidateBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val state by scanViewModel.state.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bmp = runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(stream)
            }.getOrNull()
            if (bmp != null) {
                shareCandidateBitmap = bmp
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
                title = { Text("Active Shields", color = TextPrimary, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
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
                Text("Protection Modules", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            // Share Guard
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Safe)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Share Interceptor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Scans images before they are shared to other apps.", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(checked = true, onCheckedChange = { }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Safe))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeLight, contentColor = Safe)
                        ) { Text("SIMULATE A SHARE", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Ghost Mode
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = AccentIqoo)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ghost Mode (Data Poisoning)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Silently replaces copied API keys and passwords with fake, valid-looking data.", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = true, onCheckedChange = { }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentIqoo))
                    }
                }
            }

            // Streamer Guard
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Esports Shield", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Prevents OTPs and sensitive notifications from appearing on stream via NPU.", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = true, onCheckedChange = { }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPurple))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("Test Environment", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

            // Unified Test Environment
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Simulate Clipboard Interception", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Paste text below to see Ghost Mode actively poison the data.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        
                        OutlinedTextField(
                            value = developerText,
                            onValueChange = { developerText = it },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("Paste secrets here...", color = TextTertiary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentIqoo, unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        
                        var localResult by remember { mutableStateOf<com.privacyguardian.domain.model.PrivacyRiskResult?>(null) }
                        LaunchedEffect(developerText) {
                            val entities = app.detector.detect(developerText, emptyList())
                            localResult = app.riskEngine.calculateRisk(entities)
                        }
                        
                        if (localResult != null && localResult!!.detectedEntities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Ghost Data Injected:", color = AccentIqoo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            localResult!!.detectedEntities.forEach { e ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(e.type.name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(e.maskedValue, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    var sanitized = developerText
                                    localResult!!.detectedEntities.forEach { e -> sanitized = sanitized.replace(e.originalValue, e.maskedValue) }
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("sanitized", sanitized))
                                    Toast.makeText(context, "Poisoned data copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentIqoo, contentColor = Color.White)
                            ) { Text("INJECT & COPY TO CLIPBOARD", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
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
                if (hasRisk) Text("⚠️ SENSITIVE DATA DETECTED", color = Critical, fontWeight = FontWeight.Bold)
                else Text("No sensitive data", color = Safe, fontWeight = FontWeight.Bold)
            },
            text = {
                if (hasRisk) Column {
                    Text("Exposure Risk:  / 100", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Detected:", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    risk?.detectedEntities?.take(3)?.forEach { e ->
                        Text("• : ", color = TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Text("This image appears safe to share.", color = TextSecondary)
                }
            },
            confirmButton = {
                if (hasRisk) {
                    Button(onClick = { showShareDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Safe)) { Text("PROTECT & SHARE") }
                } else {
                    Button(onClick = { showShareDialog = false }) { Text("SHARE") }
                }
            },
            dismissButton = {
                if (hasRisk) {
                    TextButton(onClick = { showShareDialog = false }) { Text("SHARE ANYWAY (UNSAFE)", color = Critical) }
                }
            }
        )
    }
}