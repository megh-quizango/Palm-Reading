package app.palm.feature.scan

import app.palm.contract.HandSide
import app.palm.core.photo.PhotoSource
import app.palm.core.vision.FillCenterMapping
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.HandFrame
import app.palm.testing.FakeScanCamera
import app.palm.testing.LandmarkFixtures
import app.palm.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    /** A ViewModel on a 1080x2400 view whose capture is open (palm held 913 ms). */
    private fun readyVm(): ScannerViewModel = palmVm().apply {
        onViewSize(1080, 2400)
        feed(frame("right_palm"), 12)
        assertTrue(ui.value.captureEnabled)
    }

    @Test fun captureDoesNothingUntilCaptureIsEnabled() = runTest(main.dispatcher) {
        val camera = FakeScanCamera()
        val vm = palmVm()
        vm.feed(frame("right_palm"), 6) // palm shown, hold not reached
        vm.capture(camera) { error("must not capture") }
        advanceUntilIdle()
        assertEquals(0, camera.takeCount)
        assertFalse(vm.ui.value.capturing)
    }

    @Test fun captureFocusesAtThePalmCentreThenTakesThePhotoOnce() = runTest(main.dispatcher) {
        val camera = FakeScanCamera()
        val vm = readyVm()
        val captured = mutableListOf<PhotoSource.Capture>()
        vm.capture(camera) { captured += it }
        assertTrue(vm.ui.value.capturing)
        assertFalse(vm.ui.value.captureEnabled)
        advanceUntilIdle()
        val f = frame("right_palm")
        val ids = listOf(0, 5, 9, 13, 17)
        val (x, y) = FillCenterMapping(480, 640, 1080, 2400)
            .imageToView(ids.map { f.points[it].x }.average().toFloat() * 480, ids.map { f.points[it].y }.average().toFloat() * 640)
        assertEquals(x, camera.focusPoints.single().first, 0.5f)
        assertEquals(y, camera.focusPoints.single().second, 0.5f)
        assertEquals(1, camera.takeCount)
        assertSame(camera.photo, captured.single())
        assertEquals(90, captured.single().rotationDegrees)
        assertFalse(vm.ui.value.capturing)
    }

    @Test fun focusThatNeverReturnsOrThrowsStillCaptures() = runTest(main.dispatcher) {
        val camera = FakeScanCamera().apply { focus = FakeScanCamera.Focus.NeverReturn }
        val vm = readyVm()
        var captured = 0
        vm.capture(camera) { captured++ }
        runCurrent()
        advanceTimeBy(999)
        runCurrent()
        assertEquals(0, camera.takeCount)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, camera.takeCount)
        assertEquals(1, captured)

        val throwing = FakeScanCamera().apply { focus = FakeScanCamera.Focus.Throw }
        val vm2 = readyVm()
        vm2.capture(throwing) { captured++ }
        advanceUntilIdle()
        assertEquals(1, throwing.takeCount)
        assertEquals(2, captured)
    }

    @Test fun captureErrorShowsForFourSecondsAndASecondTapWhileCapturingIsIgnored() = runTest(main.dispatcher) {
        val camera = FakeScanCamera().apply { failCapture = true; focus = FakeScanCamera.Focus.NeverReturn }
        val vm = readyVm()
        vm.capture(camera) { error("must not be called") }
        vm.capture(camera) { error("must not be called") }
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, camera.takeCount)
        assertTrue(vm.ui.value.captureFailed)
        assertFalse(vm.ui.value.capturing)
        advanceTimeBy(3_999)
        runCurrent()
        assertTrue(vm.ui.value.captureFailed)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(vm.ui.value.captureFailed)
    }

    @Test fun torchTogglesOnlyWithAFlashAndTorchOffAlwaysTurnsItOff() = runTest(main.dispatcher) {
        val camera = FakeScanCamera()
        val vm = palmVm()
        vm.onCameraReady(camera)
        assertTrue(vm.ui.value.hasFlash)
        vm.toggleTorch(camera)
        assertTrue(vm.ui.value.torchOn)
        vm.toggleTorch(camera)
        assertFalse(vm.ui.value.torchOn)
        vm.torchOff(camera)
        assertEquals(listOf(true, false, false), camera.torchCalls)

        val noFlash = FakeScanCamera(hasFlash = false)
        val vm2 = palmVm().apply { onCameraReady(noFlash) }
        assertFalse(vm2.ui.value.hasFlash)
        vm2.toggleTorch(noFlash)
        vm2.torchOff(noFlash)
        assertEquals(listOf(false), noFlash.torchCalls)
        assertFalse(vm2.ui.value.torchOn)
    }
}
