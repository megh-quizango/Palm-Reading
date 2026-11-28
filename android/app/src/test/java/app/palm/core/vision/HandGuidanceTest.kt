package app.palm.core.vision

import app.palm.contract.HandSide
import app.palm.testing.LandmarkFixture
import app.palm.testing.LandmarkFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Synthetic fixtures from tools/gen_landmark_fixtures.py (D-09); thresholds = GuidanceConfig.Default. */
class HandGuidanceTest {
    private fun readAll(f: LandmarkFixture, visible: VisibleRect? = f.mapping.visibleRect()): List<GuidanceReading> {
        val guidance = HandGuidance(f.hand)
        return f.handFrames().map { guidance.read(it, visible) }
    }

    private fun first(name: String) = LandmarkFixtures.load(name).handFrames().first()!!

    @Test fun everyFixtureFrameClassifiesToItsLabelForBothHands() {
        val fixtures = LandmarkFixtures.all()!!.filterNot { "dropout" in it.label }
        assertEquals(24, fixtures.size)
        for (f in fixtures) {
            readAll(f).drop(f.settleFrames).forEachIndexed { i, r ->
                assertEquals("${f.label} frame ${i + f.settleFrames}", f.expected, r.raw)
            }
        }
    }

    @Test fun halfSizeHandIsJudgedAgainstTheVisibleRectNotTheFullFrame() {
        val f = LandmarkFixtures.load("right_half")
        assertEquals(GuidanceState.PalmDetected, readAll(f).single().raw)
        assertEquals(GuidanceState.TooFar, readAll(f, visible = null).single().raw)
    }

    @Test fun transposedFrameGivesTheSameCurledFingerCount() {
        for (name in listOf("right_fist", "right_palm", "left_fist")) {
            val frame = first(name)
            val transposed = frame.copy(points = frame.points.map { P3(it.y, it.x, it.z) }, width = frame.height, height = frame.width)
            assertEquals(name, HandGuidance(HandSide.RIGHT).read(frame).curledFingers, HandGuidance(HandSide.RIGHT).read(transposed).curledFingers)
        }
    }

    @Test fun confidentOtherHandAddsWrongHandWithoutChangingRaw() {
        val r = HandGuidance(HandSide.RIGHT).read(first("left_palm").copy(handednessScore = 0.95f))
        assertEquals(GuidanceState.PalmDetected, r.raw)
        assertEquals(HandSide.LEFT, r.wrongHand)
    }

    @Test fun lowScoreOtherHandFallsBackToTheChosenHandAndReadsAsBackOfHand_mirrorAmbiguity() {
        val r = HandGuidance(HandSide.RIGHT).read(first("left_palm").copy(handednessScore = 0.5f))
        assertEquals(HandSide.RIGHT, r.identity)
        assertEquals(GuidanceState.BackOfHand, r.raw)
        assertNull(r.wrongHand)
    }

    @Test fun nullFrameIsNoHandWithoutPalmCentre() {
        val r = HandGuidance(HandSide.LEFT).read(null)
        assertEquals(GuidanceState.NoHand, r.raw)
        assertNull(r.palmCenter)
    }

    @Test fun boundarySequencesHoldTheirStateThroughHysteresis() {
        for (name in listOf("right_far_boundary", "left_far_boundary")) {
            assertTrue(name, readAll(LandmarkFixtures.load(name)).all { it.raw == GuidanceState.TooFar })
        }
        for (name in listOf("right_moving_boundary", "left_moving_boundary")) {
            assertTrue(name, readAll(LandmarkFixtures.load(name)).drop(1).all { it.raw == GuidanceState.Moving })
        }
    }

    @Test fun stillIsPalmOnEveryFrameAndMovingIsMovingFromTheSecondFrame() {
        val still = readAll(LandmarkFixtures.load("right_still"))
        assertEquals(30, still.size)
        assertTrue(still.drop(1).all { it.raw == GuidanceState.PalmDetected })
        val moving = readAll(LandmarkFixtures.load("right_moving"))
        assertEquals(GuidanceState.PalmDetected, moving.first().raw)
        assertTrue(moving.drop(1).all { it.raw == GuidanceState.Moving })
    }

    @Test fun recordedFixturesClassifyOnAtLeast90PercentOfFrames() {
        val recorded = LandmarkFixtures.all("/landmarks/recorded/")
        assumeTrue("no recorded fixtures (D-09: real hands stay local)", recorded != null)
        for (f in recorded!!) {
            val hits = readAll(f).count { it.raw == f.expected }
            assertTrue("${f.label}: $hits / ${f.frames.size}", hits >= 0.9 * f.frames.size)
        }
    }
}
