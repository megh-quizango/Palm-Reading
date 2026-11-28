package app.palm.core.reading

import android.content.Context
import app.palm.contract.ImageSizeDto
import app.palm.contract.PalmReadingDto
import app.palm.core.data.AppLanguage
import app.palm.core.photo.ProcessedCapture
import app.palm.debug.debugFixtureVariant
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
            debugFixtureVariant() ?: FixtureVariant.MIXED, // release twin is always null (D-04, D-06)
            capture,
            locale,
            Instant.now(),
        )
    }
}

/** [source] maps a fixture file name to its text (assets on Android, a directory in JVM tests). */
internal fun fixtureReading(
    source: (String) -> String,
    variant: FixtureVariant,
    capture: ProcessedCapture,
    lang: AppLanguage,
    now: Instant,
): PalmReadingDto {
    val name = when {
        variant == FixtureVariant.HINDI -> "palm_reading.hi.json"
        variant.code.startsWith("invalid_") -> "palm_reading.invalid.json"
        else -> "palm_reading.${lang.tag}.json"
    }
    val fixture = ContractJson.decodeFromString<PalmReadingDto>(source(name))
    // derive reads only the argument its variant needs, so the one decoded fixture fills all three
    val reading = variant.derive(fixture, fixture, fixture)
    // The Hindi variant's text is Hindi whatever the app language
    return reading.stamp(capture, if (variant == FixtureVariant.HINDI) AppLanguage.HI else lang, now)
}

/** D-02: a fresh id, the capture's hand and size, the app locale and the time; traces stay normalised. */
fun PalmReadingDto.stamp(capture: ProcessedCapture, lang: AppLanguage, now: Instant): PalmReadingDto = copy(
    readingId = UUID.randomUUID().toString(),
    handSide = capture.hand,
    imageSize = ImageSizeDto(capture.width, capture.height),
    meta = meta.copy(locale = lang.tag, generatedAt = now.truncatedTo(ChronoUnit.SECONDS).toString()),
)
