package app.palm.feature.scan

import app.palm.contract.HandSide
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.PhotoSource
import app.palm.core.photo.ProcessResult
import app.palm.core.photo.ScanResultHolder
import app.palm.core.vision.P3
import app.palm.testing.FakePhotoProcessor
import app.palm.testing.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScanViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())
    @get:Rule val tmp = TemporaryFolder()

    private val processor = FakePhotoProcessor()
    private val dir by lazy { File(tmp.root, "scan") }
    private val holder by lazy { ScanResultHolder(dir) }
    private val vm by lazy { ScanViewModel(processor, holder).apply { io = main.dispatcher } }
    private val capture = PhotoSource.Capture(byteArrayOf(1, 2, 3), 90)
    private val ok = ProcessResult.Ok(byteArrayOf(9, 8, 7), 960, 1280, List(21) { P3(0.5f, 0.5f, 0f) })

    private fun start(origin: PhotoOrigin = PhotoOrigin.CAMERA) = vm.process(capture, origin, HandSide.RIGHT)

    private fun noFiles() = dir.listFiles().isNullOrEmpty()

    @Test fun okStoresTheCaptureAndSignalsReadyOnce() = runTest(main.dispatcher) {
        start()
        assertTrue(vm.ui.value.processing)
        processor.next.complete(ok)
        advanceUntilIdle()
        val stored = holder.latest!!
        assertArrayEquals(ok.jpeg, stored.file.readBytes())
        assertEquals(HandSide.RIGHT, stored.hand)
        assertEquals(PhotoOrigin.CAMERA, stored.origin)
        assertFalse(vm.ui.value.processing)
        assertTrue(vm.ui.value.ready)
        vm.consumeReady()
        assertFalse(vm.ui.value.ready) // consumed once, so a recomposition does not navigate again
    }

    @Test fun noHandFromCameraIsRejectedAndKeepsNothing() = runTest(main.dispatcher) {
        start()
        processor.next.complete(ProcessResult.NoHand)
        advanceUntilIdle()
        assertEquals(Rejection(RejectReason.NoHand, PhotoOrigin.CAMERA), vm.ui.value.rejected)
        assertNull(holder.latest)
        assertTrue(noFiles())
        assertFalse(vm.ui.value.ready)
    }

    @Test fun unsupportedImportIsRejectedWithItsOrigin() = runTest(main.dispatcher) {
        start(PhotoOrigin.GALLERY)
        processor.next.complete(ProcessResult.Unsupported)
        advanceUntilIdle()
        assertEquals(Rejection(RejectReason.Unsupported, PhotoOrigin.GALLERY), vm.ui.value.rejected)
    }

    @Test fun failedIsRejectedAndDismissClearsIt() = runTest(main.dispatcher) {
        start()
        processor.next.complete(ProcessResult.Failed)
        advanceUntilIdle()
        assertEquals(Rejection(RejectReason.Failed, PhotoOrigin.CAMERA), vm.ui.value.rejected)
        vm.dismissRejection()
        assertNull(vm.ui.value.rejected)
    }

    @Test fun cancelDropsALateResult() = runTest(main.dispatcher) {
        start()
        advanceUntilIdle()
        vm.cancel()
        assertFalse(vm.ui.value.processing)
        processor.next.complete(ok)
        advanceUntilIdle()
        assertNull(holder.latest)
        assertTrue(noFiles())
        assertFalse(vm.ui.value.ready)
        assertNull(vm.ui.value.rejected)
    }

    @Test fun secondProcessWhileBusyIsIgnored() = runTest(main.dispatcher) {
        start()
        start(PhotoOrigin.GALLERY)
        advanceUntilIdle()
        assertEquals(1, processor.sources.size)
    }
}
