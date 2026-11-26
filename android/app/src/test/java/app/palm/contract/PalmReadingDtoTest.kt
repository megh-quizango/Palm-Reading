package app.palm.contract

import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Drift detector: strict decoder (ignoreUnknownKeys = false is the default). Production client Json is Phase 5.
private val strict = Json

private fun contract(name: String): String {
    val dir = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
    return File(dir, name).readText()
}

class PalmReadingDtoTest {
    @Test
    fun sampleParsesUnchanged() {
        val text = contract("palm_reading.sample.json")
        val r = strict.decodeFromString<PalmReadingDto>(text)
        assertEquals(1, r.schemaVersion)
        assertTrue(r.lines.all { l -> l.trace.all { it.size == 2 && it.all { c -> c in 0.0..1.0 } } })
        assertEquals(Json.parseToJsonElement(text), strict.encodeToJsonElement(PalmReadingDto.serializer(), r))
    }

    @Test
    fun invalidPhotoSampleParsesUnchanged() {
        val text = contract("palm_reading.invalid_photo.sample.json")
        val r = strict.decodeFromString<PalmReadingDto>(text)
        assertFalse(r.verdict.ok)
        assertNotNull(r.verdict.reason)
        assertTrue(r.lines.isEmpty())
        assertNull(r.palm)
        assertEquals(Json.parseToJsonElement(text), strict.encodeToJsonElement(PalmReadingDto.serializer(), r))
    }

    @Test
    fun allSevenReasonCodesDecode() {
        val text = contract("palm_reading.invalid_photo.sample.json")
        val codes = listOf("blurry_or_dark", "too_far", "jewelry", "bent", "fingers_tight", "incomplete", "no_palm_lines")
        val decoded = codes.map { strict.decodeFromString<PalmReadingDto>(text.replace("\"jewelry\"", "\"$it\"")).verdict.reason }
        assertEquals(InvalidReason.entries.toList(), decoded)
    }

    @Test
    fun extraKeyFailsStrictDecode() {
        val text = contract("palm_reading.sample.json").replaceFirst("{", "{\"unexpected\": 1, ")
        assertThrows(SerializationException::class.java) { strict.decodeFromString<PalmReadingDto>(text) }
    }
}
