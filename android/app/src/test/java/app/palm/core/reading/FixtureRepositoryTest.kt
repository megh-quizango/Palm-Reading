package app.palm.core.reading

import app.palm.contract.HandSide
import app.palm.contract.ImageSizeDto
import app.palm.contract.InvalidReason
import app.palm.contract.PalmReadingDto
import app.palm.core.data.AppLanguage
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.ProcessedCapture
import app.palm.core.vision.P3
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureRepositoryTest {
    private val capture = ProcessedCapture(File("capture.jpg"), 720, 1280, HandSide.LEFT, List(21) { P3(0.1f, 0.2f, 0.3f) }, PhotoOrigin.CAMERA)
    private val now = Instant.parse("2026-11-28T10:15:30.789Z")
    private val en = ContractJson.decodeFromString<PalmReadingDto>(fixtureText("palm_reading.en.json"))
    private val hi = ContractJson.decodeFromString<PalmReadingDto>(fixtureText("palm_reading.hi.json"))

    private fun read(variant: FixtureVariant = FixtureVariant.MIXED, lang: AppLanguage = AppLanguage.EN) =
        fixtureReading(::fixtureText, variant, capture, lang, now)

    @Test
    fun stampsEachReadForTheCapture() {
        val a = read()
        val b = read()
        val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(a.readingId.matches(uuid) && b.readingId.matches(uuid))
        assertNotEquals(a.readingId, b.readingId)
        assertNotEquals(en.readingId, a.readingId)
        assertEquals(HandSide.LEFT, a.handSide)
        assertEquals(ImageSizeDto(720, 1280), a.imageSize)
        assertEquals("2026-11-28T10:15:30Z", a.meta.generatedAt)
        assertEquals(en.lines.map { it.trace }, a.lines.map { it.trace })
        assertFalse(ContractJson.encodeToString(PalmReadingDto.serializer(), a).contains("landmarks"))
    }

    @Test
    fun theAppLanguagePicksTheFixtureAndLocale() {
        assertEquals("en", read(lang = AppLanguage.EN).meta.locale)
        val h = read(lang = AppLanguage.HI)
        assertEquals("hi", h.meta.locale)
        assertEquals(hi.palm!!.summary, h.palm!!.summary)
        val forced = read(FixtureVariant.HINDI, AppLanguage.EN)
        assertEquals(hi.palm!!.summary, forced.palm!!.summary)
        assertEquals("hi", forced.meta.locale)
    }

    @Test
    fun invalidVariantCarriesItsReason() {
        val r = read(FixtureVariant.INVALID_BENT)
        assertFalse(r.verdict.ok)
        assertEquals(InvalidReason.BENT, r.verdict.reason)
        assertEquals(emptyList<String>(), ReadingRules.check(r))
    }
}
