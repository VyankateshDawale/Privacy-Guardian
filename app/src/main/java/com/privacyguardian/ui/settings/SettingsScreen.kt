package com.privacyguardian.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.domain.model.GuardianMode
import com.privacyguardian.ui.components.GuardianModeSelector
import com.privacyguardian.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: PrivacyGuardianApp,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(GuardianMode.NORMAL) }
    var localStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        app.preferencesManager.guardianMode.collect { mode = it }
    }
    LaunchedEffect(Unit) {
        localStatus = app.privacyReasoner.getStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) } },
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
                Text("Guardian Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                GuardianModeSelector(mode, onSelect = {
                    mode = it
                    scope.launch {
                        app.preferencesManager.setGuardianMode(it)
                    }
                })
                Text(
                    when (mode) {
                        GuardianMode.NORMAL -> "Normal: Detect and ask user before protecting."
                        GuardianMode.STRICT -> "Strict: Auto-select protection for Critical entities but ask before final action."
                        GuardianMode.MAXIMUM -> "Maximum: Automatically redact Critical items (shows: Critical items auto-protected)."
                    },
                    color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }
            item { Divider(color = Border) }
            item {
                Text("Privacy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                SettingRow(Icons.Default.Lock, "On-device processing", "Supported content is processed locally. Sensitive image and OCR content is not uploaded to a cloud service by this prototype.")
                SettingRow(Icons.Default.Shield, localStatus, "Rule-based fallback ensures reliability if local model unavailable. Raw secrets never logged, persisted, or sent to cloud.")
                SettingRow(Icons.Default.PhoneAndroid, "History storage", "Only masked metadata and protected image Uris are stored in Room. Original images, raw OCR text, and raw secrets are never persisted.")
            }
            item { Divider(color = Border) }
            item {
                Text("About", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Why iQOO?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Privacy Guardian is designed as a phone-first privacy intelligence layer. Its architecture prioritizes fast local processing so sensitive information can be analyzed and protected without requiring cloud upload.",
                            color = TextSecondary, fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Future iQOO integration could include: system screenshot protection, system share checkpoint, clipboard protection, notification privacy, screen recording protection, camera privacy analysis, and deeper hardware acceleration — presented as future concepts, not currently implemented.",
                            color = TextTertiary, fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Core positioning: \"Privacy shouldn't require sending your private data to the cloud.\"", color = Safe, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Version 1.0 • Hackathon Prototype • iQOO 15", color = TextTertiary, fontSize = 11.sp)
                        Text("Built with Kotlin • Jetpack Compose • ML Kit • Room", color = TextTertiary, fontSize = 11.sp)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Security Guarantees", color = Critical, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Never logs passwords / keys / tokens\n• Never persists raw OCR text\n• Never stores original images\n• Never sends content to cloud APIs\n• Masks all UI representations\n• Uses regex/rules for reliability", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Safe, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
