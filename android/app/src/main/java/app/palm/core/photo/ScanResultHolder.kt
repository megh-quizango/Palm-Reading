package app.palm.core.photo

import app.palm.contract.HandSide
import app.palm.core.vision.P3
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The processed photo handed from Scan to ScanReady (D-01); never a route argument. */
data class ProcessedCapture(
    val file: File,
    val width: Int,
    val height: Int,
    val hand: HandSide,
    val landmarks: List<P3>,
    val origin: PhotoOrigin,
)

/** Holds the latest capture in memory and as [dir]/capture.jpg, the only photo the app writes (D-12). */
class ScanResultHolder(private val dir: File) {
    @Volatile var latest: ProcessedCapture? = null
        private set

    /** Blocking I/O: call off the main thread. Replaces the previous capture. */
    fun put(jpeg: ByteArray, width: Int, height: Int, hand: HandSide, landmarks: List<P3>, origin: PhotoOrigin): ProcessedCapture {
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("cannot create $dir")
        val file = File(dir, "capture.jpg")
        val tmp = File(dir, "capture.jpg.tmp")
        tmp.writeBytes(jpeg)
        // Atomic replace, so a reader never sees a half-written file (File.renameTo cannot replace on every platform)
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            tmp.delete()
            throw e
        }
        return ProcessedCapture(file, width, height, hand, landmarks, origin).also { latest = it }
    }

    /** Blocking I/O. Deletes every file in [dir], strays included, so no earlier photo survives (D-12). */
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
        latest = null
    }
}
