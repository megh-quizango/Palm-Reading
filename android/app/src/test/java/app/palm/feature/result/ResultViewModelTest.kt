package app.palm.feature.result

import androidx.compose.ui.graphics.ImageBitmap
import app.palm.contract.InvalidReason
import app.palm.contract.LineType
import app.palm.contract.PalmReadingDto
import app.palm.contract.VerdictDto
import app.palm.core.db.ReadingEntity
import app.palm.core.photo.PhotoDecoder
import app.palm.core.reading.ContractJson
import app.palm.core.reading.ReadingStore
import app.palm.testing.FakePhotoDecoder
import app.palm.testing.FakeReadingDao
import app.palm.testing.MainDispatcherRule
import app.palm.testing.stubImageBitmap
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResultViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())
    @get:Rule val tmp = TemporaryFolder()

    private val dao = FakeReadingDao()
    private val dir by lazy { File(tmp.root, "readings").apply { mkdirs() } }
    private val image = stubImageBitmap()

    private val sampleText: String by lazy {
        val dirName = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
        File(dirName, "palm_reading.sample.json").readText()
    }
    private val sample: PalmReadingDto by lazy { ContractJson.decodeFromString<PalmReadingDto>(sampleText) }

    /** A row with [json] and a photo file next to it; returns the id. */
    private suspend fun row(json: String = ContractJson.encodeToString(sample), withPhoto: Boolean = true): String {
        val id = UUID.randomUUID().toString()
        if (withPhoto) File(dir, "$id.jpg").writeBytes(byteArrayOf(1, 2, 3))
        dao.upsert(ReadingEntity(id, 1, json, "$id.jpg", "RIGHT", 1L))
        return id
    }

    /** Behaves like AndroidPhotoDecoder (null for a missing file) and waits on [gate] so intermediate states show. */
    private class GatedDecoder(val next: ImageBitmap?) : PhotoDecoder {
        val gate = CompletableDeferred<Unit>()
        override suspend fun decode(file: File, maxEdge: Int): ImageBitmap? {
            gate.await()
            return if (file.isFile) next else null
        }
    }

    private fun TestScope.states(vm: ResultViewModel): MutableList<ResultState> {
        val seen = mutableListOf<ResultState>()
        // Unconfined: advanceUntilIdle skips background work, so a dispatched collector would never see the updates
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.toList(seen) }
        return seen
    }

    private fun TestScope.vm(decoder: PhotoDecoder) = ResultViewModel(ReadingStore(dir, dao, StandardTestDispatcher(testScheduler)), decoder)

    @Test fun aSavedReadingLoadsThenItsPhoto() = runTest(main.dispatcher) {
        val id = row()
        val decoder = GatedDecoder(image)
        val vm = vm(decoder)
        val seen = states(vm)
        vm.load(id)
        advanceUntilIdle()
        assertEquals(listOf(ResultState.Loading, ResultState.Found(sample, PhotoState.Loading)), seen)
        decoder.gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(ResultState.Found(sample, PhotoState.Ready(image)), seen.last())
        assertEquals(3, seen.size)
    }

    @Test fun anUndecodablePhotoIsUnavailable() = runTest(main.dispatcher) {
        val id = row()
        val vm = vm(FakePhotoDecoder(next = null))
        val seen = states(vm)
        vm.load(id)
        advanceUntilIdle()
        assertEquals(ResultState.Found(sample, PhotoState.Unavailable), seen.last())
    }

    @Test fun aDeletedPhotoFileIsUnavailable() = runTest(main.dispatcher) {
        val id = row(withPhoto = false)
        val decoder = GatedDecoder(image).apply { gate.complete(Unit) }
        val vm = vm(decoder)
        val seen = states(vm)
        vm.load(id)
        advanceUntilIdle()
        assertEquals(ResultState.Found(sample, PhotoState.Unavailable), seen.last())
    }

    @Test fun noRowIsMissing() = runTest(main.dispatcher) {
        val vm = vm(FakePhotoDecoder(image))
        val seen = states(vm)
        vm.load(UUID.randomUUID().toString())
        advanceUntilIdle()
        assertEquals(ResultState.Missing, seen.last())
    }

    @Test fun unreadableRowsAreMissingNeverACrash() = runTest(main.dispatcher) {
        val noHeart = sample.copy(lines = sample.lines.filter { it.type != LineType.HEART })
        val invalid = sample.copy(verdict = VerdictDto(false, InvalidReason.TOO_FAR), lines = emptyList(), palm = null, love = null, career = null)
        val jsons = mapOf(
            "not json" to "{not json",
            "extra key" to sampleText.replaceFirst("{", "{\"extra\": 1,"),
            "rule" to ContractJson.encodeToString(noHeart),
            "verdict" to ContractJson.encodeToString(invalid),
        )
        for ((name, json) in jsons) {
            val decoder = FakePhotoDecoder(image)
            val vm = vm(decoder)
            val seen = states(vm)
            vm.load(row(json))
            advanceUntilIdle()
            assertEquals(name, ResultState.Missing, seen.last())
            assertTrue(name, decoder.decoded.isEmpty())
        }
    }

    @Test fun theSameIdAndUnrelatedWritesDecodeOnce() = runTest(main.dispatcher) {
        val id = row()
        val decoder = FakePhotoDecoder(image)
        val vm = vm(decoder)
        val seen = states(vm)
        vm.load(id)
        advanceUntilIdle()
        vm.load(id)
        row() // another reading saved: Room re-emits every byId flow on a table write
        advanceUntilIdle()
        assertEquals(1, decoder.decoded.size)
        assertEquals(ResultState.Found(sample, PhotoState.Ready(image)), seen.last())
    }
}
