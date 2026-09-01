package com.privacyguardian.ui.history

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.ui.components.EmptyState
import com.privacyguardian.ui.components.RiskBadge
import com.privacyguardian.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    app: PrivacyGuardianApp,
    vm: HistoryViewModel = viewModel()
) {
    val items by vm.items.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<com.privacyguardian.data.local.ScanHistoryEntity?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.init(app) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Critical)
                        }
                    }
                }
            )
        },
        containerColor = Background
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Default.History, "No history yet", "Scans and protected images will appear here.\nRaw secrets are never stored.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedItem = item },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Card)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.detectionType.ifEmpty { "Scan" }, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                RiskBadge(
                                    when (item.riskLevel) {
                                        "CRITICAL" -> com.privacyguardian.domain.model.RiskLevel.CRITICAL
                                        "HIGH" -> com.privacyguardian.domain.model.RiskLevel.HIGH
                                        "MEDIUM" -> com.privacyguardian.domain.model.RiskLevel.MEDIUM
                                        "LOW" -> com.privacyguardian.domain.model.RiskLevel.LOW
                                        else -> com.privacyguardian.domain.model.RiskLevel.SAFE
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${item.riskLevel} • ${item.action} • ${item.itemCount} item(s)", color = TextSecondary, fontSize = 11.sp)
                            Text(formatDate(item.timestamp), color = TextTertiary, fontSize = 11.sp)
                            if (item.protectedImageUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = item.protectedImageUri,
                                    contentDescription = "Protected image",
                                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Text("Protected image • Tap to view", color = Safe, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Text("Risk: ${item.riskScore}/100", color = when (item.riskLevel) {
                                "CRITICAL" -> Critical
                                "HIGH" -> HighRisk
                                "MEDIUM" -> MediumRisk
                                else -> Safe
                            }, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History?", color = TextPrimary) },
            text = { Text("This will delete all scan history. Protected image files in cache may remain until cleared by system.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { vm.clearAll { showClearDialog = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = Critical)
                ) { Text("CLEAR") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("CANCEL") }
            },
            containerColor = Card
        )
    }

    if (selectedItem != null) {
        val item = selectedItem!!
        ModalBottomSheet(onDismissRequest = { selectedItem = null }) {
            Column(modifier = Modifier.padding(20.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Safe, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Detail", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    RiskBadge(
                        when (item.riskLevel) {
                            "CRITICAL" -> com.privacyguardian.domain.model.RiskLevel.CRITICAL
                            "HIGH" -> com.privacyguardian.domain.model.RiskLevel.HIGH
                            "MEDIUM" -> com.privacyguardian.domain.model.RiskLevel.MEDIUM
                            "LOW" -> com.privacyguardian.domain.model.RiskLevel.LOW
                            else -> com.privacyguardian.domain.model.RiskLevel.SAFE
                        }
                    )
                }
                Text(item.detectionType.ifEmpty { "Scan" }, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${item.riskLevel} • ${item.action} • ${item.itemCount} item(s) • ${formatDate(item.timestamp)}", color = TextSecondary, fontSize = 12.sp)
                Text("Risk: ${item.riskScore}/100", color = when (item.riskLevel) {
                    "CRITICAL" -> Critical
                    "HIGH" -> HighRisk
                    "MEDIUM" -> MediumRisk
                    else -> Safe
                }, fontWeight = FontWeight.Black, fontSize = 14.sp)
                if (item.protectedImageUri != null) {
                    Text("Protected Image", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    AsyncImage(
                        model = item.protectedImageUri,
                        contentDescription = "Protected",
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Button(
                        onClick = {
                            try {
                                val uri = android.net.Uri.parse(item.protectedImageUri)
                                val share = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(share)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated, contentColor = TextPrimary)
                    ) { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("View Full") }
                    Button(
                        onClick = {
                            try {
                                val uri = android.net.Uri.parse(item.protectedImageUri)
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Share Safe Copy"))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = androidx.compose.ui.graphics.Color.White)
                    ) { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Share Safe Copy", fontWeight = FontWeight.Bold) }
                } else {
                    Text("No protected image — text/document scan (masked text not persisted per privacy).", color = TextSecondary, fontSize = 12.sp)
                }
                Text("Privacy: raw image, OCR text, secrets never stored — only masked metadata & protected Uri.", color = TextTertiary, fontSize = 11.sp)
            }
        }
    }
}

private fun formatDate(ts: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}
