package app.palm.core.photo

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes a stored photo; an interface so JVM tests never touch BitmapFactory (RESEARCH Pitfall 6). */
interface PhotoDecoder {
    /** Null when the file is missing or does not decode. [maxEdge] > 0 subsamples to about that long edge. */
    suspend fun decode(file: File, maxEdge: Int = 0): ImageBitmap?
}

class AndroidPhotoDecoder @Inject constructor() : PhotoDecoder {
    override suspend fun decode(file: File, maxEdge: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            if (!file.isFile) return@withContext null
            val options = BitmapFactory.Options()
            if (maxEdge > 0) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                options.inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdge)
            }
            BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
        } catch (_: IOException) {
            null
        }
    }
}
