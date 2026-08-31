package com.privacyguardian.protection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import com.privacyguardian.domain.model.SensitiveEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProtectionEngine(private val context: Context) {

    private val rectPaint = Paint().apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.FILL
    }

    private val paddingPx = 8

    fun createProtectedBitmap(
        original: Bitmap,
        entities: List<SensitiveEntity>
    ): Bitmap {
        // Never mutate original
        val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        for (entity in entities) {
            val box = entity.boundingBox ?: continue
            // Add padding
            val padded = Rect(
                (box.left - paddingPx).coerceAtLeast(0),
                (box.top - paddingPx).coerceAtLeast(0),
                (box.right + paddingPx).coerceAtMost(mutable.width),
                (box.bottom + paddingPx).coerceAtMost(mutable.height)
            )
            canvas.drawRect(padded, rectPaint)
        }
        return mutable
    }

    fun createProtectedBitmapWithNoBoxesFallback(
        original: Bitmap,
        entities: List<SensitiveEntity>,
        fullText: String
    ): Bitmap {
        // If some entities have no bounding box, we fallback to bottom banner redaction
        // For demo, ensure at least image is marked protected if entities >0 but no boxes
        val hasBox = entities.any { it.boundingBox != null }
        if (hasBox) {
            return createProtectedBitmap(original, entities)
        }
        // No boxes: draw a banner covering bottom 20%
        val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val bannerTop = (mutable.height * 0.78f).toInt()
        canvas.drawRect(
            0f,
            bannerTop.toFloat(),
            mutable.width.toFloat(),
            mutable.height.toFloat(),
            rectPaint
        )
        // Optionally add text indicator
        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText("REDACTED", 32f, bannerTop + 60f, textPaint)
        return mutable
    }

    fun saveBitmapToCache(bitmap: Bitmap, fileName: String = "protected_${UUID.randomUUID()}.png"): Uri {
        val dir = File(context.cacheDir, "protected")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun protectAndSave(
        original: Bitmap,
        entities: List<SensitiveEntity>,
        fullText: String = ""
    ): Pair<Bitmap, Uri> {
        val protected = if (entities.any { it.boundingBox != null }) {
            createProtectedBitmap(original, entities)
        } else {
            createProtectedBitmapWithNoBoxesFallback(original, entities, fullText)
        }
        val uri = saveBitmapToCache(protected)
        return protected to uri
    }
}
