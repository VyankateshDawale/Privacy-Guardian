package com.privacyguardian.ui.home

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.ui.components.*
import com.privacyguardian.ui.navigation.Screen
import com.privacyguardian.ui.onboarding.OnboardingBottomSheet
import com.privacyguardian.ui.scanner.ScanViewModel
import com.privacyguardian.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: PrivacyGuardianApp,
    onNavigate: (String) -> Unit,
    scanViewModel: ScanViewModel,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.init(app) }
    LaunchedEffect(Unit) { vm.refreshRecent() }
    val state by vm.state.collectAsState()
    var showOnDeviceSheet by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        app.preferencesManager.hasSeenOnboarding.collect { seen ->
            showOnboarding = !seen
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Privacy Guardian", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Your AI Privacy Firewall", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                PrivacyStatusCard(state.riskScore)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    OnDeviceBadge(onClick = { showOnDeviceSheet = true })
                    Text("Fast • Private • On-device", color = TextTertiary, fontSize = 11.sp)
                }
            }
            // LIVE DEMO — crazy gradient animated card
            item {
                CrazyLiveDemoCard(onClick = {
                    scanViewModel.scanDemo(context)
                    onNavigate(Screen.Result.route)
                })
            }
            item { SectionHeader("Quick Actions") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(Icons.Default.PhotoLibrary, "Scan Screenshot", "Pick image • Detect • Protect", { onNavigate(Screen.Scan.route) }, Safe)
                    QuickActionCard(Icons.Default.Image, "Scan Image", "Any image with text", { onNavigate(Screen.Scan.route) }, Safe)
                    QuickActionCard(Icons.Default.Description, "Scan Document", "Text files • Privacy report", { onNavigate(Screen.Scan.route) }, Warning)
                    QuickActionCard(Icons.Default.TextFields, "Check Text", "Paste text to analyze", { onNavigate(Screen.Scan.route) }, MediumRisk)
                    QuickActionCard(Icons.Default.Code, "Developer Guard", "Scan code & secrets", { onNavigate(Screen.Guardian.route) }, HighRisk)
                    QuickActionCard(Icons.Default.History, "Privacy History", "Review past scans", { onNavigate(Screen.History.route) }, TextSecondary)
                }
            }
            item {
                Text("Guardian Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                GuardianModeSelector(state.guardianMode, onSelect = { vm.setGuardianMode(it) })
                Text(
                    when (state.guardianMode) {
                        com.privacyguardian.domain.model.GuardianMode.NORMAL -> "Normal: Detect and ask before protecting."
                        com.privacyguardian.domain.model.GuardianMode.STRICT -> "Strict: Auto-select protection for Critical items."
                        com.privacyguardian.domain.model.GuardianMode.MAXIMUM -> "Maximum: Auto-redact Critical items."
                    },
                    color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
            item { SectionHeader("Recent Activity", subtitle = if (state.recent.isEmpty()) "No scans yet" else null) }
            if (state.recent.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No recent scans. Try LIVE DEMO or Scan.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(state.recent) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(when(item.riskLevel) {
                                "CRITICAL" -> Critical
                                "HIGH" -> HighRisk
                                "MEDIUM" -> MediumRisk
                                else -> Safe
                            }))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.detectionType.ifEmpty { "Scan" }, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                Text("${item.riskLevel} • ${item.action} • ${formatTime(item.timestamp)}", color = TextSecondary, fontSize = 11.sp)
                            }
                            Text("${item.riskScore}", color = when(item.riskLevel) {
                                "CRITICAL" -> Critical
                                "HIGH" -> HighRisk
                                "MEDIUM" -> MediumRisk
                                else -> Safe
                            }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardElevated)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Why iQOO?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Privacy shouldn't require sending your private data to the cloud. Privacy Guardian uses phone-native processing so sensitive information can be analyzed and protected without requiring cloud upload.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }

    if (showOnDeviceSheet) {
        ModalBottomSheet(onDismissRequest = { showOnDeviceSheet = false }) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 30.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Safe)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processed on device", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Privacy Guardian processes supported content locally. Sensitive image and OCR content is not uploaded to a cloud service by this prototype.", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Designed for iQOO 15 — fast local processing so privacy protection feels instant.", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }

    if (showOnboarding) {
        val scope = rememberCoroutineScope()
        OnboardingBottomSheet(onDismiss = {
            showOnboarding = false
            scope.launch { app.preferencesManager.setOnboardingSeen(true) }
        })
    }
}

private fun formatTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val mins = diff / 60000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins/60}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(ts))
    }
}
