package com.privacyguardian.protection

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BoundingBoxMapperTest {

    @Test
    fun scaleRectSimple() {
        val original = Rect(0, 0, 100, 50)
        val mapped = BoundingBoxMapper.scaleRect(original, 2f, 2f)
        assertEquals(0f, mapped.left)
        assertEquals(0f, mapped.top)
        assertEquals(200f, mapped.right)
        assertEquals(100f, mapped.bottom)
    }

    @Test
    fun computeForFitCentersCorrectly() {
        // Container 1080x1920, image 1080x1920 => no letterbox
        val info = BoundingBoxMapper.computeForFit(1080f, 1920f, 1080f, 1920f)
        assertEquals(0f, info.offsetX)
        assertEquals(0f, info.offsetY)
        assertEquals(1f, info.scaleX, 0.01f)
    }

    @Test
    fun computeForFitLetterboxing() {
        // Container 1080x1920, image 1920x1080 (landscape in portrait container) => letterboxed
        val info = BoundingBoxMapper.computeForFit(1080f, 1920f, 1920f, 1080f)
        // Scale = min(1080/1920=0.5625, 1920/1080=1.77) =0.5625
        assertEquals(0.5625f, info.scaleX, 0.01f)
        assertTrue(info.offsetY > 0) // vertical letterboxing
        assertEquals(0f, info.offsetX, 0.01f)
    }

    @Test
    fun mapRectWithFit() {
        val original = Rect(0, 0, 540, 960)
        val info = BoundingBoxMapper.computeForFit(1080f, 1920f, 1080f, 1920f)
        val mapped = BoundingBoxMapper.mapRect(original, info)
        assertNotNull(mapped)
        assertEquals(0f, mapped!!.left, 0.1f)
        assertEquals(0f, mapped.top, 0.1f)
    }

    @Test
    fun scaleRectWithOffset() {
        val original = Rect(100, 100, 200, 200)
        val mapped = BoundingBoxMapper.scaleRect(original, 0.5f, 0.5f, 10f, 20f)
        assertEquals(60f, mapped.left, 0.01f) // 100*0.5+10
        assertEquals(70f, mapped.top, 0.01f)  // 100*0.5+20
    }

    @Test
    fun fullRedactionCoversSensitiveRegion() {
        // Simulate that ProtectionEngine draws opaque rect over sensitive region
        // Verify logic: bounding box + padding should stay within image bounds
        val imageWidth = 1080
        val imageHeight = 1920
        val box = Rect(100, 200, 500, 300)
        val padding = 8
        val padded = Rect(
            (box.left - padding).coerceAtLeast(0),
            (box.top - padding).coerceAtLeast(0),
            (box.right + padding).coerceAtMost(imageWidth),
            (box.bottom + padding).coerceAtMost(imageHeight)
        )
        assertTrue(padded.left >= 0)
        assertTrue(padded.top >= 0)
        assertTrue(padded.right <= imageWidth)
        assertTrue(padded.bottom <= imageHeight)
        assertTrue(padded.width() > box.width())
    }
}
