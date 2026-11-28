package app.palm.feature.result.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
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

/** content:// URI from the non-exported provider; throws IllegalArgumentException outside cacheDir/share (T-05-12). */
fun shareUri(context: Context, file: File): Uri =
    // packageName, never a literal: debug and release ids differ by the .debug suffix
    FileProvider.getUriForFile(context, context.packageName + ".share", file)

fun shareIntent(uri: Uri, text: String): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    putExtra(Intent.EXTRA_TEXT, text)
    clipData = ClipData.newRawUri(null, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

/** The grant is repeated on the chooser itself so it survives the hop through the system sheet (RESEARCH A2). */
fun shareChooser(send: Intent, title: String): Intent = Intent.createChooser(send, title).apply {
    clipData = send.clipData
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
