package app.palm.testing

import androidx.compose.ui.graphics.ImageBitmap
import app.palm.core.photo.PhotoDecoder
import java.io.File
import java.lang.reflect.Proxy

/** Returns [next] for every decode and records the files asked for. */
class FakePhotoDecoder(var next: ImageBitmap? = null) : PhotoDecoder {
    val decoded = mutableListOf<File>()

    override suspend fun decode(file: File, maxEdge: Int): ImageBitmap? {
        decoded += file
        return next
    }
}

/**
 * A non-null ImageBitmap for JVM tests (no Android bitmap behind it). Only width and height answer; a proxy keeps it
 * compiling whatever members the Compose version adds to the interface.
 */
fun stubImageBitmap(width: Int = 960, height: Int = 1280): ImageBitmap =
    Proxy.newProxyInstance(ImageBitmap::class.java.classLoader, arrayOf(ImageBitmap::class.java)) { proxy, method, args ->
        when (method.name) {
            "getWidth" -> width
            "getHeight" -> height
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            "toString" -> "StubImageBitmap(${width}x$height)"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ImageBitmap
