package app.palm.core.vision

import app.palm.contract.HandSide
import app.palm.core.vision.GuidanceState.Moving
import app.palm.core.vision.GuidanceState.NoHand
import app.palm.core.vision.GuidanceState.PalmDetected
import app.palm.core.vision.GuidanceState.TooFar
import app.palm.testing.LandmarkFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D-08 dwell / dropout / hold, driven only by the timestamps passed in. */
class GuidanceStabilizerTest {
    private val s = GuidanceStabilizer()

    /** PalmDetected shown at t=250, so capture opens at 750. */
    private fun showPalm(start: Long = 0) {
        s.update(PalmDetected, null, start)
        assertEquals(PalmDetected, s.update(PalmDetected, null, start + 250).shown)
    }

    /** Runs a fixture through HandGuidance and the stabiliser, returning every StableGuidance. */
    private fun run(name: String): List<StableGuidance> {
        val f = LandmarkFixtures.load(name)
        val g = HandGuidance(f.hand)
        return f.handFrames().zip(f.frames).map { (frame, raw) ->
            val r = g.read(frame, f.mapping.visibleRect())
            s.update(r.raw, r.wrongHand, raw.t)
        }
    }

    @Test fun rawStateIsShownAfterItPersisted250msNotBefore() {
        s.update(PalmDetected, null, 0)
        assertEquals(NoHand, s.update(PalmDetected, null, 249).shown)
        assertEquals(PalmDetected, s.update(PalmDetected, null, 250).shown)
    }

    @Test fun flipFlopEvery83msNeverChangesShown() {
        showPalm()
        for (i in 1..40) {
            assertEquals(PalmDetected, s.update(if (i % 2 == 0) PalmDetected else Moving, null, 250L + i * 83).shown)
        }
    }

    @Test fun alternatingTwoNonPalmStatesStillEndsPalmAndClosesCapture() {
        showPalm()
        assertTrue(s.update(PalmDetected, null, 800).captureEnabled)
        val out = (1..6).map { i -> s.update(if (i % 2 == 0) NoHand else TooFar, null, 800L + i * 83) }
        // Raw left PalmDetected at 883; frame 4 (1,132) is 249 ms later, frame 5 (1,215) switches.
        assertTrue(out.take(4).all { it.shown == PalmDetected })
        assertTrue(out.drop(4).all { it.shown != PalmDetected && !it.captureEnabled })
    }

    @Test fun stillSequenceChangesShownExactlyOnce() {
        val shown = run("right_still").map { it.shown }
        assertEquals(listOf(NoHand, PalmDetected), shown.distinct())
        assertEquals(PalmDetected, shown.last())
    }

    @Test fun captureOpens500msAfterPalmIsShown() {
        showPalm()
        assertFalse(s.update(PalmDetected, null, 749).captureEnabled)
        assertTrue(s.update(PalmDetected, null, 750).captureEnabled)
    }

    @Test fun noHandFor250msKeepsPalmAndCapture() {
        val out = run("right_dropout_short")
        assertTrue(out.drop(11).all { it.shown == PalmDetected && it.captureEnabled }) // shown at 332 ms, capture from frame 11 (913 ms)
        showPalm(10_000)
        s.update(PalmDetected, null, 10_800)
        s.update(NoHand, null, 10_900)
        val r = s.update(NoHand, null, 11_150)
        assertEquals(PalmDetected, r.shown)
        assertTrue(r.captureEnabled)
    }

    @Test fun noHandFor400msEndsPalmOnceItLasted300ms() {
        showPalm()
        s.update(PalmDetected, null, 800)
        s.update(NoHand, null, 900)
        assertEquals(PalmDetected, s.update(NoHand, null, 1_199).shown)
        val r = s.update(NoHand, null, 1_200)
        assertEquals(NoHand, r.shown)
        assertFalse(r.captureEnabled)
        assertEquals(NoHand, run("right_dropout_long").last().shown)
    }

    @Test fun wrongHandHintUsesTheSameDwell() {
        showPalm()
        assertNull(s.update(PalmDetected, HandSide.LEFT, 300).wrongHand)
        assertNull(s.update(PalmDetected, HandSide.LEFT, 549).wrongHand)
        assertEquals(HandSide.LEFT, s.update(PalmDetected, HandSide.LEFT, 550).wrongHand)
        assertEquals(HandSide.LEFT, s.update(PalmDetected, null, 600).wrongHand)
        assertNull(s.update(PalmDetected, null, 850).wrongHand)
    }

    @Test fun onlyTheTimestampsPassedInCount() {
        val base = 5_000_000_000L
        s.update(PalmDetected, null, base)
        assertEquals(PalmDetected, s.update(PalmDetected, null, base + 250).shown)
        assertTrue(s.update(PalmDetected, null, base + 750).captureEnabled)
    }
}
