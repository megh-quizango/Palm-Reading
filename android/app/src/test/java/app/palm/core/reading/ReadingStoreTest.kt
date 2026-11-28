package app.palm.core.reading

import app.palm.contract.HandSide
import app.palm.contract.PalmReadingDto
import app.palm.contract.VerdictDto
import app.palm.core.data.AppLanguage
import app.palm.core.db.ReadingEntity
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.ProcessedCapture
import app.palm.core.vision.P3
import app.palm.testing.FakeReadingDao
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReadingStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val dao = FakeReadingDao()
    private val dir by lazy { File(tmp.root, "readings") }
    private var clock = 1_000L

    private fun TestScope.store() = ReadingStore(dir, dao, StandardTestDispatcher(testScheduler)) { clock }

    private fun capture(bytes: ByteArray = byteArrayOf(1, 2, 3)): ProcessedCapture {
        val scan = File(tmp.root, "scan").apply { mkdirs() }
        val file = File(scan, "capture.jpg").apply { writeBytes(bytes) }
        return ProcessedCapture(file, 960, 1280, HandSide.RIGHT, List(21) { P3(0.5f, 0.5f, 0f) }, PhotoOrigin.CAMERA)
    }

    private fun sample(capture: ProcessedCapture): PalmReadingDto {
        val dirName = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
        val text = File(dirName, "palm_reading.sample.json").readText()
        return ContractJson.decodeFromString<PalmReadingDto>(text).stamp(capture, AppLanguage.EN, Instant.parse("2026-11-28T10:00:00Z"))
    }

    private inline fun <reified T : Throwable> assertIs(e: Throwable?) = assertTrue("got $e", e is T)

    private fun readingsFiles(): List<String> = dir.listFiles()?.map { it.name }.orEmpty()

    @Test fun saveMovesThePhotoAndWritesOneRow() = runTest {
        val c = capture(byteArrayOf(7, 8, 9))
        val reading = sample(c)
        val id = store().save(reading, c)
        assertEquals(reading.readingId, id)
        assertFalse(c.file.exists())
        assertArrayEquals(byteArrayOf(7, 8, 9), File(dir, "$id.jpg").readBytes())
        val row = dao.all().single()
        assertEquals("$id.jpg", row.photoFile)
        assertEquals("RIGHT", row.handSide)
        assertEquals(1_000L, row.createdAt)
        assertEquals(reading, ContractJson.decodeFromString<PalmReadingDto>(row.json))
        assertFalse(row.json.contains("landmarks"))
    }

    @Test fun unsafeIdsAreRefusedBeforeAnyFileWork() = runTest {
        val upper = UUID.randomUUID().toString().uppercase()
        for (bad in listOf("../x", "not-a-uuid", upper)) {
            val c = capture()
            val reading = sample(c).copy(readingId = bad)
            assertIs<IllegalArgumentException>(runCatching { store().save(reading, c) }.exceptionOrNull())
            assertTrue(bad, c.file.exists())
            assertTrue(bad, readingsFiles().isEmpty())
            assertTrue(File(tmp.root, "x.jpg").exists().not())
        }
        assertTrue(dao.all().isEmpty())
    }

    @Test fun invalidVerdictIsRefused() = runTest {
        val c = capture()
        val reading = sample(c).copy(verdict = VerdictDto(false, null))
        assertIs<IllegalArgumentException>(runCatching { store().save(reading, c) }.exceptionOrNull())
        assertTrue(c.file.exists())
        assertTrue(readingsFiles().isEmpty())
    }

    @Test fun failedInsertLeavesNoPhoto() = runTest {
        dao.failUpsert = true
        val c = capture()
        val reading = sample(c)
        assertIs<IOException>(runCatching { store().save(reading, c) }.exceptionOrNull())
        assertTrue(readingsFiles().isEmpty())
        assertTrue(dao.all().isEmpty())
    }

    @Test fun savingTheSameIdTwiceKeepsOneRowAndOneFile() = runTest {
        val first = capture()
        val reading = sample(first)
        store().save(reading, first)
        store().save(reading, capture(byteArrayOf(4)))
        assertEquals(1, dao.all().size)
        assertEquals(listOf("${reading.readingId}.jpg"), readingsFiles())
        assertArrayEquals(byteArrayOf(4), File(dir, "${reading.readingId}.jpg").readBytes())
    }

    @Test fun byIdAndLatestResolveThePhotoInsideTheFolder() = runTest {
        val s = store()
        val c1 = capture()
        val older = s.save(sample(c1), c1)
        clock = 2_000L
        val c2 = capture()
        val newer = s.save(sample(c2), c2)
        val saved = s.byId(older).first()!!
        assertEquals(File(dir, "$older.jpg").canonicalPath, saved.photo!!.canonicalPath)
        assertEquals(HandSide.RIGHT, saved.hand)
        assertEquals(newer, s.latest().first()!!.id)
        assertNull(s.byId(UUID.randomUUID().toString()).first())
    }

    @Test fun aPhotoFileOutsideTheFolderResolvesToNull() = runTest {
        val id = UUID.randomUUID().toString()
        dao.upsert(ReadingEntity(id, 1, "{}", "../evil.jpg", "LEFT", 5))
        val saved = store().byId(id).first()!!
        assertNull(saved.photo)
        assertEquals(HandSide.LEFT, saved.hand)
    }

    @Test fun deleteRemovesTheRowAndThePhoto() = runTest {
        val s = store()
        val c = capture()
        val id = s.save(sample(c), c)
        s.delete(id)
        assertNull(s.byId(id).first())
        assertFalse(File(dir, "$id.jpg").exists())
    }

    @Test fun deleteAllRemovesRowsPhotosAndOrphans() = runTest {
        val s = store()
        repeat(2) {
            val c = capture()
            s.save(sample(c), c)
        }
        File(dir, "orphan.jpg").writeBytes(byteArrayOf(1))
        s.deleteAll()
        assertTrue(dao.all().isEmpty())
        assertTrue(readingsFiles().isEmpty())
        assertNull(s.latest().first())
    }
}
