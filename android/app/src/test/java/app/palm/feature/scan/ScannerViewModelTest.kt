package app.palm.feature.scan

import app.palm.contract.HandSide
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.HandFrame
import app.palm.testing.LandmarkFixtures
import app.palm.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScannerViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    private fun frame(name: String) = LandmarkFixtures.load(name).handFrames().first()!!

    /** Feeds [frame] every 83 ms from [start] for [count] frames; returns the next timestamp. */
    private fun ScannerViewModel.feed(frame: HandFrame, count: Int, start: Long = 0): Long {
        var t = start
        repeat(count) {
            onFrame(frame.copy(timestampMs = t), t)
            t += 83
        }
        return t
    }

    private fun palmVm(hand: HandSide = HandSide.RIGHT) = ScannerViewModel().apply { setHand(hand) }

    @Test fun palmIsShownAfterTheDwellAndCaptureOpensAfterTheHold() = runTest(main.dispatcher) {
        val vm = palmVm()
        val palm = frame("right_palm")
        vm.feed(palm, 4) // t = 0..249
        assertEquals(GuidanceState.NoHand, vm.ui.value.shown)
        vm.onFrame(palm, 332)
        assertEquals(GuidanceState.PalmDetected, vm.ui.value.shown)
        assertFalse(vm.ui.value.captureEnabled)
        vm.onFrame(palm, 831)
        assertFalse(vm.ui.value.captureEnabled)
        vm.onFrame(palm, 832)
        assertTrue(vm.ui.value.captureEnabled)
    }

    @Test fun backOfHandAfterPalmClosesCaptureAndWrongHandHintShows() = runTest(main.dispatcher) {
        val vm = palmVm()
        val t = vm.feed(frame("right_palm"), 12)
        assertTrue(vm.ui.value.captureEnabled)
        vm.feed(frame("right_back"), 5, t)
        assertEquals(GuidanceState.BackOfHand, vm.ui.value.shown)
        assertFalse(vm.ui.value.captureEnabled)

        val other = palmVm()
        other.feed(frame("left_palm"), 6)
        assertEquals(GuidanceState.PalmDetected, other.ui.value.shown)
        assertEquals(HandSide.LEFT, other.ui.value.wrongHand)
    }

    @Test fun viewSizeDecidesTooFarAgainstTheVisibleRegion() = runTest(main.dispatcher) {
        val half = frame("right_half")
        val withView = palmVm().apply { onViewSize(1080, 2400) }
        withView.feed(half, 6)
        assertEquals(GuidanceState.PalmDetected, withView.ui.value.shown)
        val noView = palmVm()
        noView.feed(half, 6)
        assertEquals(GuidanceState.TooFar, noView.ui.value.shown)
    }

    @Test fun identicalFramesDoNotEmitPerFrameAndDebugCarriesTheLastReading() = runTest(main.dispatcher) {
        val vm = palmVm()
        val palm = frame("right_palm")
        val seen = mutableListOf(vm.ui.value)
        var t = 0L
        repeat(20) {
            vm.onFrame(palm.copy(timestampMs = t), t)
            if (vm.ui.value != seen.last()) seen += vm.ui.value
            t += 83
        }
        assertTrue("ui changed ${seen.size - 1} times", seen.size - 1 <= 2) // NoHand -> PalmDetected -> capture open
        val debug = vm.debug.value!!
        assertEquals(t - 83, debug.frame!!.timestampMs)
        assertEquals(GuidanceState.PalmDetected, debug.reading.raw)
    }

    @Test fun guideErrorDisablesCaptureWhateverTheFramesSay() = runTest(main.dispatcher) {
        val vm = palmVm()
        vm.onGuideError()
        vm.feed(frame("right_palm"), 12)
        assertTrue(vm.ui.value.guideUnavailable)
        assertFalse(vm.ui.value.captureEnabled)
    }
}
