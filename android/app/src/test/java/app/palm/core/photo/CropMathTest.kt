package app.palm.core.photo

import app.palm.core.vision.P3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {
    private fun box(lo: Float, hi: Float) = List(21) { i -> if (i % 2 == 0) P3(lo, lo, 0f) else P3(hi, hi, 0f) }

    @Test
    fun cropGrowsTheLandmarkBoxByTheMargin() {
        assertEquals(CropRect(125, 100, 875, 700), cropFor(box(0.25f, 0.75f), 1000, 800, 0.25f))
    }

    @Test
    fun cropClampsToTheImage() {
        assertEquals(CropRect(0, 0, 1000, 800), cropFor(box(0f, 1f), 1000, 800, 0.25f))
    }

    @Test
    fun remapMapsCropCornersAndScalesZ() {
        val crop = CropRect(125, 100, 875, 700)
        assertEquals(P3(0f, 0f, 0f), remapToCrop(P3(0.125f, 0.125f, 0f), 1000, 800, crop))
        val far = remapToCrop(P3(0.875f, 0.875f, 0.3f), 1000, 800, crop)
        assertEquals(1f, far.x, 1e-5f)
        assertEquals(1f, far.y, 1e-5f)
        assertEquals(0.3f * 1000 / 750, far.z, 1e-5f)
    }

    @Test
    fun scaledSizeCapsTheLongEdgeAndNeverUpscales() {
        assertEquals(1280 to 960, scaledSize(4000, 3000))
        assertEquals(800 to 600, scaledSize(800, 600))
    }

    @Test
    fun sampleSizeIsThePowerOfTwoThatFitsTheDecodeEdge() {
        assertEquals(4, sampleSizeFor(10000, 10000))
        assertEquals(1, sampleSizeFor(3072, 2000))
        assertEquals(2, sampleSizeFor(3073, 10))
    }

    @Test
    fun landmarksValidFollowsTheBackendRule() {
        val ok = List(21) { P3(it / 20f, 1f - it / 20f, 0f) }
        assertTrue(landmarksValid(ok))
        assertFalse(landmarksValid(ok.take(20)))
        assertFalse(landmarksValid(ok.toMutableList().also { it[3] = P3(Float.NaN, 0.5f, 0f) }))
        assertFalse(landmarksValid(ok.toMutableList().also { it[3] = P3(1.6f, 0.5f, 0f) }))
        assertTrue(landmarksValid(ok.toMutableList().also { it[3] = P3(-0.5f, 0.5f, 0f) }))
    }

    @Test
    fun exifTransformCoversAllEightOrientations() {
        val want = mapOf(
            1 to ExifTransform(0, false), 2 to ExifTransform(0, true),
            3 to ExifTransform(180, false), 4 to ExifTransform(180, true),
            5 to ExifTransform(90, true), 6 to ExifTransform(90, false),
            7 to ExifTransform(270, true), 8 to ExifTransform(270, false),
            0 to ExifTransform(0, false), 9 to ExifTransform(0, false),
        )
        want.forEach { (orientation, t) -> assertEquals("orientation $orientation", t, exifTransform(orientation)) }
    }
}
