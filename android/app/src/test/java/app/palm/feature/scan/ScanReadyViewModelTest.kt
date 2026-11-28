package app.palm.feature.scan

import app.palm.contract.HandSide
import app.palm.contract.InvalidReason
import app.palm.contract.LineType
import app.palm.contract.PalmReadingDto
import app.palm.contract.VerdictDto
import app.palm.core.data.AppLanguage
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.ProcessedCapture
import app.palm.core.photo.ScanResultHolder
import app.palm.core.reading.ContractJson
import app.palm.core.reading.ReadingStore
import app.palm.core.reading.stamp
import app.palm.core.vision.P3
import app.palm.testing.FakeAppLocale
import app.palm.testing.FakePalmReadingRepository
import app.palm.testing.FakePhotoDecoder
import app.palm.testing.FakeReadingDao
import app.palm.testing.MainDispatcherRule
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Every ScanReady read outcome (D-03, D-15; RESEARCH Pitfalls 2 and 11). */
class ScanReadyViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())
    @get:Rule val tmp = TemporaryFolder()

    private val dao = FakeReadingDao()
    private val repository = FakePalmReadingRepository()
    private val locale = FakeAppLocale(AppLanguage.EN)
    private val scanDir by lazy { File(tmp.root, "scan") }
    private val readingsDir by lazy { File(tmp.root, "readings") }
    private val holder by lazy { ScanResultHolder(scanDir) }
    private val store by lazy { ReadingStore(readingsDir, dao, main.dispatcher) { 1_000L } }
    private lateinit var capture: ProcessedCapture
    private val vm by lazy {
        ScanReadyViewModel(holder, repository, store, locale, FakePhotoDecoder()).apply { io = main.dispatcher }
    }

    private fun put(): ProcessedCapture =
        holder.put(byteArrayOf(1, 2, 3), 960, 1280, HandSide.RIGHT, List(21) { P3(0.5f, 0.5f, 0f) }, PhotoOrigin.CAMERA)
            .also { capture = it }

    private fun contract(name: String): PalmReadingDto {
        val dir = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
        return ContractJson.decodeFromString<PalmReadingDto>(File(dir, name).readText())
    }

    private fun valid() = contract("palm_reading.sample.json").stamp(capture, AppLanguage.EN, Instant.parse("2026-11-28T10:00:00Z"))

    private fun invalid(reason: InvalidReason) = contract("palm_reading.invalid_photo.sample.json")
        .copy(verdict = VerdictDto(false, reason))
        .stamp(capture, AppLanguage.EN, Instant.parse("2026-11-28T10:00:00Z"))

    private fun readingsFiles() = readingsDir.listFiles()?.map { it.name }.orEmpty()

    @Test fun validReadingIsSavedOnceAndHandedOverOnce() = runTest(main.dispatcher) {
        put()
        vm.read()
        assertEquals(ReadState.Reading, vm.readState.value)
        val reading = valid()
        repository.next.complete(reading)
        advanceUntilIdle()
        assertEquals(ReadState.Saved(reading.readingId), vm.readState.value)
        assertEquals(listOf(reading.readingId), dao.all().map { it.id })
        assertEquals(listOf("${reading.readingId}.jpg"), readingsFiles())
        assertFalse(capture.file.exists())
        assertNull(holder.latest)
        vm.consumeSaved()
        assertEquals(ReadState.Idle, vm.readState.value)
        vm.consumeSaved()
        advanceUntilIdle()
        assertEquals(ReadState.Idle, vm.readState.value)
    }

    @Test fun doubleTapReadsOnce() = runTest(main.dispatcher) {
        put()
        vm.read()
        vm.read()
        advanceUntilIdle()
        assertEquals(1, repository.calls.size)
    }

    @Test fun retakeWhileReadingKeepsTheCapture() = runTest(main.dispatcher) {
        put()
        vm.read()
        vm.retake()
        advanceUntilIdle()
        assertTrue(capture.file.exists())
        assertEquals(capture, holder.latest)
    }

    @Test fun everyInvalidReasonSavesNothingAndDeletesTheCapture() = runTest(main.dispatcher) {
        for (reason in InvalidReason.entries) {
            put()
            val model = ScanReadyViewModel(holder, repository, store, locale, FakePhotoDecoder()).apply { io = main.dispatcher }
            repository.next = CompletableDeferred()
            model.read()
            repository.next.complete(invalid(reason))
            advanceUntilIdle()
            assertEquals(reason.name, ReadState.Invalid(reason, HandSide.RIGHT), model.readState.value)
            assertTrue(reason.name, dao.all().isEmpty())
            assertTrue(reason.name, readingsFiles().isEmpty())
            assertFalse(reason.name, capture.file.exists())
            assertNull(reason.name, holder.latest)
            model.consumeInvalid()
            assertEquals(ReadState.Idle, model.readState.value)
            val before = repository.calls.size
            model.read() // the capture is gone, so nothing is read again
            advanceUntilIdle()
            assertEquals(before, repository.calls.size)
        }
    }

    @Test fun invalidAttemptLeavesTheEarlierReadingAsLatest() = runTest(main.dispatcher) {
        put()
        val first = valid()
        store.save(first, capture)
        put()
        vm.read()
        repository.next.complete(invalid(InvalidReason.JEWELRY))
        advanceUntilIdle()
        assertEquals(ReadState.Invalid(InvalidReason.JEWELRY, HandSide.RIGHT), vm.readState.value)
        assertEquals(first.readingId, store.latest().first()?.id)
        assertEquals(1, dao.all().size)
        assertEquals(listOf("${first.readingId}.jpg"), readingsFiles())
    }

    @Test fun failureKeepsTheCaptureAndTryAgainReadsAgain() = runTest(main.dispatcher) {
        put()
        vm.read()
        repository.next.completeExceptionally(IOException("offline"))
        advanceUntilIdle()
        assertEquals(ReadState.Failed, vm.readState.value)
        assertTrue(dao.all().isEmpty())
        assertTrue(capture.file.exists())
        repository.next = CompletableDeferred()
        vm.read()
        val reading = valid()
        repository.next.complete(reading)
        advanceUntilIdle()
        assertEquals(2, repository.calls.size)
        assertEquals(ReadState.Saved(reading.readingId), vm.readState.value)
        assertEquals(1, dao.all().size)
    }

    @Test fun ruleBreakingReadingFailsAndSavesNothing() = runTest(main.dispatcher) {
        put()
        vm.read()
        val reading = valid()
        repository.next.complete(reading.copy(lines = reading.lines.filter { it.type != LineType.HEART }))
        advanceUntilIdle()
        assertEquals(ReadState.Failed, vm.readState.value)
        assertTrue(dao.all().isEmpty())
        assertTrue(readingsFiles().isEmpty())
        assertTrue(capture.file.exists())
    }

    @Test fun failedSaveEndsInFailedWithNoFileLeft() = runTest(main.dispatcher) {
        put()
        dao.failUpsert = true
        vm.read()
        repository.next.complete(valid())
        advanceUntilIdle()
        assertEquals(ReadState.Failed, vm.readState.value)
        assertTrue(dao.all().isEmpty())
        assertTrue(readingsFiles().isEmpty())
    }

    @Test fun repositoryGetsTheAppLocale() = runTest(main.dispatcher) {
        put()
        locale.currentValue = AppLanguage.HI
        vm.read()
        advanceUntilIdle()
        assertEquals(listOf(AppLanguage.HI), repository.locales)
    }

    @Test fun noAppLocaleMeansEnglish() = runTest(main.dispatcher) {
        put()
        locale.currentValue = null
        vm.read()
        advanceUntilIdle()
        assertEquals(listOf(AppLanguage.EN), repository.locales)
    }
}
