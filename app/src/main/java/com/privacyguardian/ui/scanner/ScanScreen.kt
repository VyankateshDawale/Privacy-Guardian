package com.privacyguardian.ui.scanner

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.ui.components.*
import com.privacyguardian.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    app: PrivacyGuardianApp,
    scanViewModel: ScanViewModel,
    onResult: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val state by scanViewModel.state.collectAsState()
    var showOnDevice by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var showTextScanner by remember { mutableStateOf(false) }
    var showDocPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { scanViewModel.init(app) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            scanViewModel.scanUri(context, cameraUri!!)
            onResult()
        } else if (!success) {
            Toast.makeText(context, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scanViewModel.scanUri(context, uri)
            onResult()
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // Read text/plain
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                if (text.isBlank()) {
                    Toast.makeText(context, "Empty or unsupported document", Toast.LENGTH_SHORT).show()
                } else {
                    scanViewModel.scanTextInput(text)
                    onResult()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                actions = {
                    IconButton(onClick = { showOnDevice = true }) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Safe)
                    }
                }
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OnDeviceBadge(onClick = { showOnDevice = true })
                }
            }
            if (state.isLoading) {
                item { LoadingState(state.stage) }
            }
            if (state.error != null) {
                item { ErrorState(state.error!!, onRetry = { scanViewModel.clearError() }) }
            }
            item {
                QuickActionCard(Icons.Default.PhotoLibrary, "Scan Screenshot", "Pick from gallery • Auto OCR", {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, Safe)
            }
            item {
                QuickActionCard(Icons.Default.Image, "Scan Image", "Any image containing text", {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, Safe)
            }
            item {
                QuickActionCard(Icons.Default.PhotoCamera, "Camera Scanner", "Capture → OCR → Protect (on-device)", {
                    requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                }, AccentBlue)
            }
            item {
                QuickActionCard(Icons.Default.Description, "Scan Document", "Text files (MVP)", {
                    docPicker.launch(arrayOf("text/plain"))
                }, Warning)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Card)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.TextFields, contentDescription = null, tint = MediumRisk)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Text", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Paste text, code, or secrets here...", color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Safe,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Safe
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (textInput.isBlank()) {
                                    Toast.makeText(context, "Enter text to scan", Toast.LENGTH_SHORT).show()
                                } else {
                                    scanViewModel.scanTextInput(textInput)
                                    onResult()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = androidx.compose.ui.graphics.Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCAN TEXT", fontWeight = FontWeight.Bold)
                        }
                        if (state.riskResult != null && state.originalBitmap == null && state.ocrResult != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val sanitized = scanViewModel.getSanitizedText()
                            Text("Sanitized preview:", color = TextSecondary, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth().background(CardElevated, RoundedCornerShape(8.dp)).padding(10.dp)) {
                                Text(sanitized.take(500), color = TextPrimary, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("sanitized", sanitized)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied Safe Version", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CardElevated, contentColor = TextPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("COPY SANITIZED TEXT")
                            }
                        }
                    }
                }
            }
            item {
                // Demo fallback
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Try the Live Demo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("No image? Use the demo screenshot with fake secrets to see the full flow instantly.", color = TextSecondary, fontSize = 12.sp)
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
}
