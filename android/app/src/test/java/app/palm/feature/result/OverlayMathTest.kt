package app.palm.feature.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayMathTest {
    private val eps = 1e-4f

    @Test fun fitRectFillsBoxesAtThePhotoAspect() {
        assertEquals(FitRect(0f, 0f, 360f, 480f), fitRect(960, 1280, 360f, 480f))
        assertEquals(FitRect(0f, 0f, 360f, 360f), fitRect(1080, 1080, 360f, 360f))
        val tall = fitRect(720, 1280, 270f, 480f)
        assertEquals(0f, tall.left, eps)
        assertEquals(0f, tall.top, eps)
        assertEquals(270f, tall.width, eps)
        assertEquals(480f, tall.height, eps)
    }

    @Test fun fitRectLetterboxesWhenTheHeightIsCapped() {
        val r = fitRect(960, 1280, 360f, 300f)
        assertEquals(225f, r.width, eps)
        assertEquals(300f, r.height, eps)
        assertEquals(67.5f, r.left, eps)
        assertEquals(0f, r.top, eps)
    }

    @Test fun mapClampsAndCentres() {
        val r = fitRect(960, 1280, 360f, 300f)
        assertEquals(r.left to r.top + r.height, r.map(-0.2, 1.5))
        val (cx, cy) = r.map(0.5, 0.5)
        assertEquals(180f, cx, eps)
        assertEquals(150f, cy, eps)
    }

    @Test fun catmullRomEndsEachCubicOnTheNextPoint() {
        val px = floatArrayOf(0f, 10f, 25f, 30f, 50f)
        val py = floatArrayOf(0f, 8f, 3f, 20f, 22f)
        val cubics = catmullRom(px, py)
        assertEquals(4, cubics.size)
        cubics.forEachIndexed { i, c ->
            assertEquals(px[i + 1], c.x, 0f)
            assertEquals(py[i + 1], c.y, 0f)
        }
    }

    private fun cross(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)

    @Test fun twoPointsGiveAStraightSegment() {
        val c = catmullRom(floatArrayOf(1f, 9f), floatArrayOf(2f, 6f)).single()
        assertEquals(0f, cross(1f, 2f, 9f, 6f, c.c1x, c.c1y), eps)
        assertEquals(0f, cross(1f, 2f, 9f, 6f, c.c2x, c.c2y), eps)
        assertEquals(9f, c.x, 0f)
        assertEquals(6f, c.y, 0f)
    }

    @Test fun collinearPointsStayCollinear() {
        val cubics = catmullRom(floatArrayOf(0f, 4f, 10f), floatArrayOf(0f, 2f, 5f))
        assertEquals(2, cubics.size)
        for (c in cubics) {
            assertEquals(0f, cross(0f, 0f, 10f, 5f, c.c1x, c.c1y), eps)
            assertEquals(0f, cross(0f, 0f, 10f, 5f, c.c2x, c.c2y), eps)
        }
    }

    @Test fun catmullRomNeedsTwoPoints() {
        assertThrows(IllegalArgumentException::class.java) { catmullRom(floatArrayOf(1f), floatArrayOf(1f)) }
        assertThrows(IllegalArgumentException::class.java) { catmullRom(floatArrayOf(), floatArrayOf()) }
    }

    @Test fun capPointsKeepsShortTracesAndSamplesLongOnes() {
        val short = List(10) { listOf(it / 10.0, 0.5) }
        assertEquals(short, capPoints(short))
        val long = List(500) { listOf(it / 500.0, it / 1000.0) }
        val capped = capPoints(long)
        assertEquals(64, capped.size)
        assertEquals(long.first(), capped.first())
        assertEquals(long.last(), capped.last())
        assertEquals(capped, capped.distinct())
    }

    @Test fun aspectMismatchUsesATwoPercentBoundary() {
        assertFalse(aspectMismatch(960, 1280, 900, 1200))
        assertTrue(aspectMismatch(960, 1280, 960, 960))
        assertFalse(aspectMismatch(1000, 1000, 1019, 1000))
        assertTrue(aspectMismatch(1000, 1000, 1021, 1000))
        assertTrue(aspectMismatch(1021, 1000, 1000, 1000))
    }
}
