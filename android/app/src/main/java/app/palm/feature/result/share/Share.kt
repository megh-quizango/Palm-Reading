package app.palm.feature.result.share

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes [image] as cacheDir/share/reading.png, one file overwritten atomically (D-22, T-05-13). PNG carries no EXIF. */
suspend fun writeSharePng(cacheDir: File, image: ImageBitmap, io: CoroutineDispatcher = Dispatchers.IO): File = withContext(io) {
    // API 28+ captures are HARDWARE bitmaps, which cannot be compressed directly (RESEARCH Pitfall 4)
    val bitmap = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false) ?: throw IOException("bitmap copy failed")
    try {
        val dir = File(cacheDir, "share").apply { mkdirs() }
        val tmp = File(dir, "reading.png.tmp")
        val ok = tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!ok) throw IOException("png encode failed")
        val file = File(dir, "reading.png")
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        file
    } finally {
        bitmap.recycle()
    }
}
