package com.privacyguardian.ui.components

import android.graphics.Rect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.domain.model.SensitiveType
import com.privacyguardian.protection.BoundingBoxMapper
import com.privacyguardian.ui.theme.Critical
import com.privacyguardian.ui.theme.HighRisk
import com.privacyguardian.ui.theme.MediumRisk
import com.privacyguardian.ui.theme.Safe

data class BoxOverlay(
    val rect: Rect,
    val riskLevel: RiskLevel,
    val label: String = ""
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

    // Pulse animation for CRITICAL boxes
    val infiniteTransition = rememberInfiniteTransition(label = "boxPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        boxes.forEach { box ->
            val mapped = BoundingBoxMapper.mapRect(box.rect, displayInfo) ?: return@forEach
            val color = when (box.riskLevel) {
                RiskLevel.CRITICAL -> Critical
                RiskLevel.HIGH -> HighRisk
                RiskLevel.MEDIUM -> MediumRisk
                else -> Safe
            }
            val fillAlpha = if (box.riskLevel == RiskLevel.CRITICAL) pulseAlpha else 0.18f
            val cornerRadius = CornerRadius(6.dp.toPx())

            // Fill background
            drawRoundRect(
                color = color.copy(alpha = fillAlpha),
                topLeft = Offset(mapped.left, mapped.top),
                size = Size(mapped.width(), mapped.height()),
                cornerRadius = cornerRadius
            )
            // Border stroke
            drawRoundRect(
                color = color.copy(alpha = 0.9f),
                topLeft = Offset(mapped.left, mapped.top),
                size = Size(mapped.width(), mapped.height()),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.2.dp.toPx())
            )

            // Label above the box
            if (box.label.isNotBlank()) {
                drawIntoCanvas { canvas ->
                    val labelText = box.label
                    val labelTextSize = 11.dp.toPx()
                    val textPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        this.textSize = labelTextSize
                        this.isAntiAlias = true
                        this.isFakeBoldText = true
                        this.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val textWidth = textPaint.measureText(labelText)
                    val labelHeight = labelTextSize + 4.dp.toPx()
                    val labelLeft = mapped.left
                    val labelTop = (mapped.top - labelHeight - 2.dp.toPx()).coerceAtLeast(0f)
                    val labelEnd = (labelLeft + textWidth + 10.dp.toPx()).coerceAtMost(containerWidth)

                    // Label background pill
                    val bgPaint = android.graphics.Paint().apply {
                        this.color = color.copy(alpha = 0.92f).run {
                            android.graphics.Color.argb(
                                (alpha * 255).toInt(),
                                (red * 255).toInt(),
                                (green * 255).toInt(),
                                (blue * 255).toInt()
                            )
                        }
                        this.isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        labelLeft,
                        labelTop,
                        labelEnd,
                        labelTop + labelHeight,
                        4.dp.toPx(), 4.dp.toPx(),
                        bgPaint
                    )
                    // Label text
                    canvas.nativeCanvas.drawText(
                        labelText,
                        labelLeft + 5.dp.toPx(),
                        labelTop + labelTextSize,
                        textPaint
                    )
                }
            }
        }
    }
}
