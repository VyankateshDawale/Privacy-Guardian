package com.privacyguardian.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.data.local.ScanHistoryEntity
import com.privacyguardian.detection.SensitiveDataDetector
import com.privacyguardian.domain.model.PrivacyRiskResult
import com.privacyguardian.ocr.OcrElement
import com.privacyguardian.ocr.OcrResult
import com.privacyguardian.protection.ProtectionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

data class ScanUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val originalBitmap: Bitmap? = null,
    val originalUri: Uri? = null,
    val ocrResult: OcrResult? = null,
    val riskResult: PrivacyRiskResult? = null,
    val protectedBitmap: Bitmap? = null,
    val protectedUri: Uri? = null,
    val stage: String = "Idle"
)

class ScanViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    private var app: PrivacyGuardianApp? = null
    private var detector: SensitiveDataDetector? = null
    private var ocrEngine: com.privacyguardian.ocr.MlKitOcrEngine? = null
    private var riskEngine: com.privacyguardian.risk.RiskEngine? = null

    fun init(app: PrivacyGuardianApp) {
        this.app = app
        this.detector = app.detector
        this.ocrEngine = app.ocrEngine
        this.riskEngine = app.riskEngine
    }

    fun setError(msg: String) {
        _state.value = _state.value.copy(isLoading = false, error = msg)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun reset() {
        _state.value = ScanUiState()
    }

    // Load bitmap from Uri with downsampling
    suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()
            // Re-open
            val input2 = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val maxDim = 2048
            var scale = 1
            while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) scale *= 2
            val opts2 = BitmapFactory.Options().apply { inSampleSize = scale }
            val bmp = BitmapFactory.decodeStream(input2, null, opts2)
            input2.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadDemoBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.assets.open("demo_screenshot.png").use { s ->
                BitmapFactory.decodeStream(s)
            }
        } catch (_: Exception) {
            // fallback to drawable
            try {
                val resId = context.resources.getIdentifier("demo_screenshot", "drawable", context.packageName)
                if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
            } catch (_: Exception) { null }
        }
    }

    fun scanBitmap(context: Context, bitmap: Bitmap, uri: Uri? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, stage = "Analyzing", originalBitmap = bitmap, originalUri = uri)
            try {
                _state.value = _state.value.copy(stage = "Detecting text")
                val ocrResult = withContext(Dispatchers.IO) {
                    val result = ocrEngine?.recognize(bitmap)
                    result?.getOrNull()
                }
                if (ocrResult == null) {
                    _state.value = _state.value.copy(isLoading = false, error = "Couldn't analyze this image. Try another image.", stage = "Error")
                    return@launch
                }
                if (ocrResult.fullText.isBlank()) {
                    _state.value = _state.value.copy(isLoading = false, ocrResult = ocrResult, stage = "No text found", error = null)
                    // Still run detection (will be empty) to show 0 risk
                }
                _state.value = _state.value.copy(ocrResult = ocrResult, stage = "Understanding context")

                val elementsForDetector = ocrResult.elements.map {
                    SensitiveDataDetector.OcrElementRef(it.text, it.boundingBox)
                }

                _state.value = _state.value.copy(stage = "Determining risk")
                val entities = detector?.detect(ocrResult.fullText, elementsForDetector) ?: emptyList()
                val risk = riskEngine?.calculateRisk(entities)

                _state.value = _state.value.copy(
                    isLoading = false,
                    riskResult = risk,
                    stage = "Complete"
                )

                // Update prefs last risk
                if (risk != null) {
                    app?.preferencesManager?.setLastRiskScore(risk.score)
                    // Save history entry (without raw text/image)
                    if (entities.isNotEmpty() || risk.score > 0) {
                        val detectionType = entities.take(2).joinToString(", ") { it.type.name } .ifEmpty { "Clean" }
                        val action = "Scanned"
                        app?.scanHistoryRepository?.insert(
                            ScanHistoryEntity(
                                timestamp = System.currentTimeMillis(),
                                detectionType = detectionType,
                                riskLevel = risk.riskLevel.name,
                                riskScore = risk.score,
                                action = action,
                                protectedImageUri = null,
                                itemCount = entities.size
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Scan failed", stage = "Error")
            }
        }
    }

    fun scanUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, stage = "Loading image", error = null)
            val bmp = loadBitmap(context, uri)
            if (bmp == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Unable to load image")
                return@launch
            }
            scanBitmap(context, bmp, uri)
        }
    }

    fun scanTextInput(text: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, stage = "Analyzing text")
            try {
                // Create pseudo OCR result for text input (no bounding boxes)
                val pseudoElements = text.split("\n").map { OcrElement(it, null) }
                val ocrResult = OcrResult(fullText = text, elements = pseudoElements, imageWidth = 0, imageHeight = 0)
                val entities = detector?.detect(text, emptyList()) ?: emptyList()
                val risk = riskEngine?.calculateRisk(entities)
                _state.value = _state.value.copy(isLoading = false, ocrResult = ocrResult, riskResult = risk, stage = "Complete", originalBitmap = null)
                if (risk != null) {
                    app?.preferencesManager?.setLastRiskScore(risk.score)
                    if (entities.isNotEmpty()) {
                        app?.scanHistoryRepository?.insert(
                            ScanHistoryEntity(
                                timestamp = System.currentTimeMillis(),
                                detectionType = entities.take(2).joinToString(", ") { it.type.name },
                                riskLevel = risk.riskLevel.name,
                                riskScore = risk.score,
                                action = "Text Scan",
                                protectedImageUri = null,
                                itemCount = entities.size
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed")
            }
        }
    }

    fun scanDemo(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, stage = "Loading demo", error = null)
            val bmp = loadDemoBitmap(context)
            if (bmp == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Demo image not found")
                return@launch
            }
            scanBitmap(context, bmp, null)
        }
    }

    fun protectCurrent(context: Context) {
        val s = _state.value
        val bmp = s.originalBitmap ?: return
        val entities = s.riskResult?.detectedEntities ?: return
        if (entities.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, stage = "Protecting")
            try {
                val engine = app?.protectionEngine() ?: ProtectionEngine(context)
                val (protectedBmp, uri) = withContext(Dispatchers.IO) {
                    engine.protectAndSave(bmp, entities, s.ocrResult?.fullText ?: "")
                }
                _state.value = _state.value.copy(isLoading = false, protectedBitmap = protectedBmp, protectedUri = uri, stage = "Protected")
                // Save history with protected uri
                val risk = s.riskResult
                if (risk != null) {
                    app?.scanHistoryRepository?.insert(
                        ScanHistoryEntity(
                            timestamp = System.currentTimeMillis(),
                            detectionType = entities.take(2).joinToString(", ") { it.type.name },
                            riskLevel = risk.riskLevel.name,
                            riskScore = risk.score,
                            action = "Protected",
                            protectedImageUri = uri.toString(),
                            itemCount = entities.size
                        )
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Protection failed")
            }
        }
    }

    fun getSanitizedText(): String {
        val text = _state.value.ocrResult?.fullText ?: return ""
        var sanitized = text
        _state.value.riskResult?.detectedEntities?.forEach { e ->
            sanitized = sanitized.replace(e.originalValue, e.maskedValue)
        }
        return sanitized
    }

    // For sharing guard simulation
    fun simulateShareProtection(context: Context, bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val ocr = withContext(Dispatchers.IO) { ocrEngine?.recognize(bitmap)?.getOrNull() }
            if (ocr == null) {
                _state.value = _state.value.copy(isLoading = false)
                onResult(false); return@launch
            }
            val entities = detector?.detect(ocr.fullText, ocr.elements.map { SensitiveDataDetector.OcrElementRef(it.text, it.boundingBox) }) ?: emptyList()
            val risk = riskEngine?.calculateRisk(entities)
            _state.value = _state.value.copy(ocrResult = ocr, riskResult = risk, originalBitmap = bitmap, isLoading = false)
            onResult(entities.isNotEmpty() && (risk?.score ?: 0) >= 35)
        }
    }
}
