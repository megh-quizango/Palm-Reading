package app.palm.core.reading

import android.content.Context
import app.palm.contract.ImageSizeDto
import app.palm.contract.PalmReadingDto
import app.palm.core.data.AppLanguage
import app.palm.core.photo.ProcessedCapture
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The reading seam (D-01): the fixture now, the backend in Phase 6 (one @Binds in ReadingModule). */
interface PalmReadingRepository {
    /** Throws on failure. */
    suspend fun read(capture: ProcessedCapture, locale: AppLanguage): PalmReadingDto
}

/** Reads the bundled fixture and stamps it for the capture; no artificial delay. */
class FixturePalmReadingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PalmReadingRepository {
    override suspend fun read(capture: ProcessedCapture, locale: AppLanguage): PalmReadingDto = withContext(Dispatchers.IO) {
        fixtureReading(
            { name -> context.assets.open("fixtures/$name").use { it.readBytes().decodeToString() } },
            capture,
            locale,
            Instant.now(),
        )
    }
}

/** [source] maps a fixture file name to its text (assets on Android, a directory in JVM tests). */
internal fun fixtureReading(source: (String) -> String, capture: ProcessedCapture, lang: AppLanguage, now: Instant): PalmReadingDto =
    ContractJson.decodeFromString<PalmReadingDto>(source("palm_reading.en.json")).stamp(capture, lang, now)

/** D-02: a fresh id, the capture's hand and size, the app locale and the time; traces stay normalised. */
fun PalmReadingDto.stamp(capture: ProcessedCapture, lang: AppLanguage, now: Instant): PalmReadingDto = copy(
    readingId = UUID.randomUUID().toString(),
    handSide = capture.hand,
    imageSize = ImageSizeDto(capture.width, capture.height),
    meta = meta.copy(locale = lang.tag, generatedAt = now.truncatedTo(ChronoUnit.SECONDS).toString()),
)
