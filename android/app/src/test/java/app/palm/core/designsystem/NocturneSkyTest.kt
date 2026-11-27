package app.palm.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NocturneSkyTest {
    private val sky = makeSky()

    @Test fun layerCountsMatchTheSpec() {
        assertEquals(55, sky.far.size)
        assertTrue(sky.far.all { it.alpha in 0.25f..0.55f })
        assertEquals(26, sky.mid.size)
        assertTrue(sky.mid.all { it.alpha in 0.5f..0.9f })
        assertEquals(listOf(5, 3), sky.constellations.map { it.size })
        assertEquals(7, sky.sparkles.size)
        assertTrue(sky.sparkles.all { it.radiusDp in 3.5f..6.5f })
        assertEquals(3, sky.nebula.size)
        assertEquals("fixed seed", sky, makeSky())
    }

    @Test fun twinkleNeverDropsBelowFloor() {
        val stars = sky.far + sky.mid
        for (t in 0L..60_000L step 37L) stars.forEach { s ->
            val a = twinkleAlpha(s, t)
            assertTrue("alpha $a at $t", a >= 0.25f && a <= 0.9f + 1e-4f)
        }
    }

    @Test fun oneSparkleAtATimeAndEveryOneGetsATurn() {
        val seen = HashSet<Int>()
        for (t in 0L..60_000L step 50L) {
            val i = activeSparkle(t)
            assertTrue("index $i at $t", i in -1..6)
            if (i >= 0) seen += i
        }
        val cycle = HashSet<Int>()
        for (t in 0L until SPARKLE_CYCLE_MS step 50L) activeSparkle(t).takeIf { it >= 0 }?.let { cycle += it }
        assertEquals((0..6).toSet(), cycle)
        assertEquals((0..6).toSet(), seen)
    }

    @Test fun shootingStarsLast900msNeverOverlapAndAre8To12sApart() {
        for (k in 0 until 20) {
            val gap = shootingStarStart(k + 1) - shootingStarStart(k)
            assertTrue("gap $gap after window $k", gap in 8_000L..12_000L)
        }
        // Visible exactly inside each 900 ms window and nowhere else
        val end = shootingStarStart(20)
        var k = 0
        for (t in 0L until end step 10L) {
            while (t >= shootingStarStart(k) + SHOOTING_STAR_MS) k++
            val inside = t >= shootingStarStart(k)
            assertEquals("t=$t", inside, shootingStar(t) >= 0f)
        }
    }

    @Test fun restStateConstants() {
        assertEquals(0.45f, REST_CONSTELLATION_ALPHA)
        assertEquals(0.85f, REST_NEBULA_ALPHA)
        assertEquals(1f, REST_SPARKLE_SCALE)
        assertEquals(1f, sparkleScale(0f), 1e-6f)
        assertEquals(1f, sparkleScale(1f), 1e-6f)
        val scales = (0..100).map { sparkleScale(it / 100f) }
        assertEquals(1.15f, scales.max(), 0.01f)
        assertEquals(0.8f, scales.min(), 0.01f)
    }
}
