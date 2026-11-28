package app.palm.testing

import app.palm.core.photo.PhotoProcessor
import app.palm.core.photo.PhotoSource
import app.palm.core.photo.ProcessResult
import kotlinx.coroutines.CompletableDeferred

/** Each process() call suspends on [next] until the test completes it; every source is recorded. */
class FakePhotoProcessor : PhotoProcessor {
    var next = CompletableDeferred<ProcessResult>()
    val sources = mutableListOf<PhotoSource>()

    override suspend fun process(source: PhotoSource): ProcessResult {
        sources += source
        return next.await()
    }
}
