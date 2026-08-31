package com.privacyguardian.ocr

import android.graphics.Rect

data class OcrElement(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float? = null
)

data class OcrResult(
    val fullText: String,
    val elements: List<OcrElement>,
    val imageWidth: Int,
    val imageHeight: Int
)

sealed class OcrState {
    object Idle : OcrState()
    object Loading : OcrState()
    data class Success(val result: OcrResult) : OcrState()
    data class Error(val message: String) : OcrState()
    object NoText : OcrState()
}
