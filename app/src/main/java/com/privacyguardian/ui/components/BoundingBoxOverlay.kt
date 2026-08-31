package com.privacyguardian.ui.components

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.privacyguardian.protection.BoundingBoxMapper
import com.privacyguardian.ui.theme.Critical
import com.privacyguardian.ui.theme.HighRisk
import com.privacyguardian.ui.theme.MediumRisk
import com.privacyguardian.ui.theme.Safe
import com.privacyguardian.domain.model.RiskLevel

data class BoxOverlay(
    val rect: Rect,
    val riskLevel: RiskLevel
)

@Composable
fun BoundingBoxOverlay(
    boxes: List<BoxOverlay>,
    imageWidth: Int,
    imageHeight: Int,
    containerWidth: Float,
    containerHeight: Float,
    modifier: Modifier = Modifier
) {
    if (boxes.isEmpty() || imageWidth <= 0 || imageHeight <= 0 || containerWidth <= 0 || containerHeight <= 0) return
    val displayInfo = BoundingBoxMapper.computeForFit(containerWidth, containerHeight, imageWidth.toFloat(), imageHeight.toFloat())
    Canvas(modifier = modifier.fillMaxSize()) {
        boxes.forEach { box ->
            val mapped = BoundingBoxMapper.mapRect(box.rect, displayInfo) ?: return@forEach
            val color = when (box.riskLevel) {
                RiskLevel.CRITICAL -> Critical
                RiskLevel.HIGH -> HighRisk
                RiskLevel.MEDIUM -> MediumRisk
                else -> Safe
            }
            // Fill with translucent + border
            drawRect(
                color = color.copy(alpha = 0.22f),
                topLeft = Offset(mapped.left, mapped.top),
                size = Size(mapped.width(), mapped.height())
            )
            drawRect(
                color = color,
                topLeft = Offset(mapped.left, mapped.top),
                size = Size(mapped.width(), mapped.height()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
