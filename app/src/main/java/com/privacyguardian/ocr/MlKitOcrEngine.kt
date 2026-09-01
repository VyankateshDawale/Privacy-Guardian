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
            val fullText = visionText.text

            if (fullText.isBlank()) {
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
            // Track seen texts to avoid exact duplicates
            val seenTexts = mutableSetOf<String>()

            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    // Add line-level element (for multi-word sensitive data spanning a line)
                    val lineText = line.text
                    val lineBox = line.boundingBox?.let { Rect(it) }
                    if (lineText.isNotBlank()) {
                        val key = "${lineText}_${lineBox?.left}_${lineBox?.top}"
                        if (seenTexts.add(key)) {
                            elements.add(OcrElement(lineText, lineBox))
                        }
                    }
                    // Add word-level elements for precise bounding boxes
                    for (element in line.elements) {
                        val elemText = element.text
                        val elemBox = element.boundingBox?.let { Rect(it) }
                        if (elemText.isNotBlank()) {
                            val key = "${elemText}_${elemBox?.left}_${elemBox?.top}"
                            if (seenTexts.add(key)) {
                                elements.add(OcrElement(elemText, elemBox))
                            }
                        }
                    }
                }
                // Also add block-level for very long values (e.g. JWT across block)
                val blockText = block.text
                val blockBox = block.boundingBox?.let { Rect(it) }
                if (blockText.isNotBlank() && blockText.length > 20) {
                    val key = "${blockText.take(40)}_block"
                    if (seenTexts.add(key)) {
                        elements.add(OcrElement(blockText, blockBox))
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
            recognize(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        try { recognizer.close() } catch (_: Exception) {}
    }
}
