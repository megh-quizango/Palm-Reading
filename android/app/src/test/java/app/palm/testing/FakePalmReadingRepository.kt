package app.palm.testing

import app.palm.contract.PalmReadingDto
import app.palm.core.data.AppLanguage
import app.palm.core.photo.ProcessedCapture
import app.palm.core.reading.PalmReadingRepository
import kotlinx.coroutines.CompletableDeferred

/** Each read() suspends on [next] (complete it exceptionally for a failure); every call is recorded. */
class FakePalmReadingRepository : PalmReadingRepository {
    var next = CompletableDeferred<PalmReadingDto>()
    val calls = mutableListOf<ProcessedCapture>()
    val locales = mutableListOf<AppLanguage>()

    override suspend fun read(capture: ProcessedCapture, locale: AppLanguage): PalmReadingDto {
        calls += capture
        locales += locale
        return next.await()
    }
}
