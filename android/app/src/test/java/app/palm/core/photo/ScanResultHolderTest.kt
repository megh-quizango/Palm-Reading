package app.palm.core.photo

import app.palm.contract.HandSide
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScanResultHolderTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun put(holder: ScanResultHolder, bytes: ByteArray) =
        holder.put(bytes, 10, 20, HandSide.LEFT, emptyList(), PhotoOrigin.GALLERY)

    @Test fun putWritesCaptureJpg() {
        val dir = File(tmp.root, "scan")
        val stored = put(ScanResultHolder(dir), byteArrayOf(1, 2))
        assertEquals(File(dir, "capture.jpg"), stored.file)
        assertArrayEquals(byteArrayOf(1, 2), stored.file.readBytes())
    }

    @Test fun secondPutLeavesOneFileWithTheNewBytes() {
        val dir = File(tmp.root, "scan")
        val holder = ScanResultHolder(dir)
        put(holder, byteArrayOf(1, 2))
        put(holder, byteArrayOf(3, 4, 5))
        val files = dir.listFiles()!!
        assertEquals(1, files.size)
        assertArrayEquals(byteArrayOf(3, 4, 5), files[0].readBytes())
    }

    @Test fun clearDeletesEveryFileIncludingStrays() {
        val dir = File(tmp.root, "scan")
        val holder = ScanResultHolder(dir)
        put(holder, byteArrayOf(1))
        File(dir, "capture.jpg.tmp").writeBytes(byteArrayOf(2))
        File(dir, "old.jpg").writeBytes(byteArrayOf(3))
        holder.clear()
        assertTrue(dir.listFiles()!!.isEmpty())
        assertNull(holder.latest)
    }

    @Test fun clearOnAMissingDirDoesNotThrow() {
        val holder = ScanResultHolder(File(tmp.root, "never-created"))
        holder.clear()
        assertNull(holder.latest)
    }
}
