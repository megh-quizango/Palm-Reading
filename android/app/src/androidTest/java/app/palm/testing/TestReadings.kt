package app.palm.testing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import app.palm.contract.HandSide
import app.palm.contract.PalmReadingDto
import app.palm.core.data.AppLanguage
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.ProcessedCapture
import app.palm.core.reading.ContractJson
import app.palm.core.reading.ReadingStore
import app.palm.core.reading.stamp
import app.palm.core.vision.P3
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Saves a fixture reading for a plain JPEG of [width] x [height] through [store] and returns its id.
 * Uses the public stamp(), not fixtureReading(), whose internal signature 05-05 changes. Delete the id after the test.
 */
suspend fun seedReading(
    context: Context,
    store: ReadingStore,
    hand: HandSide = HandSide.RIGHT,
    width: Int = 960,
    height: Int = 1280,
    edit: (PalmReadingDto) -> PalmReadingDto = { it },
): String {
    val dir = File(context.cacheDir, "test-${UUID.randomUUID()}").apply { mkdirs() }
    val file = File(dir, "capture.jpg")
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 170, 150)) }
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    bitmap.recycle()
    val capture = ProcessedCapture(file, width, height, hand, List(21) { P3(0.5f, 0.5f, 0f) }, PhotoOrigin.GALLERY)
    val text = context.assets.open("fixtures/palm_reading.en.json").use { it.readBytes().decodeToString() }
    val reading = edit(ContractJson.decodeFromString<PalmReadingDto>(text).stamp(capture, AppLanguage.EN, Instant.now()))
    try {
        return store.save(reading, capture)
    } finally {
        dir.deleteRecursively()
    }
}
