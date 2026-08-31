package com.privacyguardian.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MlKitOcrEngine : OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(bitmap: Bitmap): Result<OcrResult> {
        return try {
            if (bitmap.isRecycled) {
                return Result.failure(IllegalArgumentException("Bitmap is recycled"))
            }
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            val fullText = visionText.text ?: ""

            if (fullText.isBlank()) {
                // Return empty but success with no text flag handled upstream
                return Result.success(
                    OcrResult(
                        fullText = "",
                        elements = emptyList(),
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height
                    )
                )
            }

            val elements = mutableListOf<OcrElement>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val text = element.text ?: continue
                        val box = element.boundingBox?.let { Rect(it) }
                        elements.add(OcrElement(text, box))
                    }
                    // Also add line bounding box for broader coverage
                    // If line element not split, keep line text
                }
                // Fallback: also capture lines
                for (line in block.lines) {
                    val box = line.boundingBox?.let { Rect(it) }
                    // Avoid duplicate if element already covers same text
                    if (line.text.isNotBlank()) {
                        // Don't duplicate single word lines heavily; but keep for bbox mapping
                    }
                }
            }
            // If elements empty but we have text, create pseudo elements per line
            if (elements.isEmpty() && fullText.isNotBlank()) {
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        elements.add(OcrElement(line.text ?: "", line.boundingBox?.let { Rect(it) }))
                    }
                }
            }

            Result.success(
                OcrResult(
                    fullText = fullText,
                    elements = elements,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recognize(uri: Uri, loadBitmap: suspend (Uri) -> Bitmap?): Result<OcrResult> {
        return try {
            val bitmap = loadBitmap(uri) ?: return Result.failure(IllegalArgumentException("Unable to load image"))
            val result = recognize(bitmap)
            // Caller handles bitmap recycling if needed; we don't recycle here
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        try { recognizer.close() } catch (_: Exception) {}
    }
}
