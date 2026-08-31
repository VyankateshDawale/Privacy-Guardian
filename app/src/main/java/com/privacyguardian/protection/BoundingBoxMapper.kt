package com.privacyguardian.protection

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object BoundingBoxMapper {

    data class DisplayInfo(
        val displayedWidth: Float,
        val displayedHeight: Float,
        val imageWidth: Float,
        val imageHeight: Float,
        // For ContentScale.Fit: actual image rect inside display
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f
    )

    /**
     * For ContentScale.Fit: compute actual image display rect and letterboxing offsets.
     * displayWidth/displayHeight is the container size.
     * imageWidth/imageHeight is original bitmap size.
     */
    fun computeForFit(
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): DisplayInfo {
        if (containerWidth <= 0 || containerHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            return DisplayInfo(containerWidth, containerHeight, imageWidth, imageHeight)
        }
        val scale = min(containerWidth / imageWidth, containerHeight / imageHeight)
        val displayedW = imageWidth * scale
        val displayedH = imageHeight * scale
        val offsetX = (containerWidth - displayedW) / 2f
        val offsetY = (containerHeight - displayedH) / 2f
        return DisplayInfo(
            displayedWidth = containerWidth,
            displayedHeight = containerHeight,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            offsetX = offsetX,
            offsetY = offsetY,
            scaleX = scale,
            scaleY = scale
        )
    }

    /**
     * Simple scaling without letterbox (ContentScale.FillBounds / Crop)
     */
    fun computeSimple(
        displayedWidth: Float,
        displayedHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): DisplayInfo {
        val sx = if (imageWidth > 0) displayedWidth / imageWidth else 1f
        val sy = if (imageHeight > 0) displayedHeight / imageHeight else 1f
        return DisplayInfo(displayedWidth, displayedHeight, imageWidth, imageHeight, 0f, 0f, sx, sy)
    }

    fun mapRect(original: Rect?, displayInfo: DisplayInfo): RectF? {
        if (original == null) return null
        val left = original.left * displayInfo.scaleX + displayInfo.offsetX
        val top = original.top * displayInfo.scaleY + displayInfo.offsetY
        val right = original.right * displayInfo.scaleX + displayInfo.offsetX
        val bottom = original.bottom * displayInfo.scaleY + displayInfo.offsetY
        return RectF(left, top, right, bottom)
    }

    fun mapRects(originals: List<Rect>, displayInfo: DisplayInfo): List<RectF> {
        return originals.mapNotNull { mapRect(it, displayInfo) }
    }

    // For testing / pure scaling
    fun scaleRect(original: Rect, scaleX: Float, scaleY: Float, offsetX: Float = 0f, offsetY: Float = 0f): RectF {
        return RectF(
            original.left * scaleX + offsetX,
            original.top * scaleY + offsetY,
            original.right * scaleX + offsetX,
            original.bottom * scaleY + offsetY
        )
    }
}
