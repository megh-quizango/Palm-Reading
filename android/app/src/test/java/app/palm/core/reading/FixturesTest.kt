package app.palm.core.reading

import app.palm.contract.InvalidReason
import app.palm.contract.LineType
import app.palm.contract.PalmReadingDto
import app.palm.contract.TraceSource
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun fixtureText(name: String): String {
    val dir = requireNotNull(System.getProperty("fixtures.dir")) { "fixtures.dir system property not set" }
    return File(dir, name).readText()
}

private fun contractText(name: String): String {
    val dir = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
    return File(dir, name).readText()
}

private fun decode(text: String) = ContractJson.decodeFromString<PalmReadingDto>(text)

class FixturesTest {
    private val en = decode(fixtureText("palm_reading.en.json"))
    private val hi = decode(fixtureText("palm_reading.hi.json"))
    private val invalid = decode(fixtureText("palm_reading.invalid.json"))

    @Test
    fun enAndInvalidFixturesEqualTheContractSamples() {
        assertEquals(Json.parseToJsonElement(contractText("palm_reading.sample.json")), Json.parseToJsonElement(fixtureText("palm_reading.en.json")))
        assertEquals(
            Json.parseToJsonElement(contractText("palm_reading.invalid_photo.sample.json")),
            Json.parseToJsonElement(fixtureText("palm_reading.invalid.json")),
        )
    }

    @Test
    fun hindiFixtureMirrorsTheEnglishStructure() {
        assertEquals(emptyList<String>(), ReadingRules.check(hi))
        assertEquals("hi", hi.meta.locale)
        assertEquals(en.imageSize, hi.imageSize)
        assertEquals(en.verdict, hi.verdict)
        assertEquals(en.lines.map { it.type }, hi.lines.map { it.type })
        assertEquals(en.lines.map { it.present }, hi.lines.map { it.present })
        assertEquals(en.lines.map { it.trace }, hi.lines.map { it.trace })
        assertEquals(en.lines.map { it.traceSource }, hi.lines.map { it.traceSource })
        assertNotEquals(en.palm!!.summary, hi.palm!!.summary)
        assertNotEquals(en.readingId, hi.readingId)
    }

    @Test
    fun everyVariantIsAValidContractReading() {
        assertEquals(14, FixtureVariant.entries.size)
        for (v in FixtureVariant.entries) {
            for (app in listOf(en, hi)) {
                val r = v.derive(app, hi, invalid)
                assertEquals("$v", r, decode(ContractJson.encodeToString(PalmReadingDto.serializer(), r)))
                assertEquals("$v", emptyList<String>(), ReadingRules.check(r))
            }
        }
    }

    @Test
    fun eachVariantHasThePropertyItsNamePromises() {
        fun d(v: FixtureVariant) = v.derive(en, hi, invalid)
        for ((v, src) in listOf(FixtureVariant.ALL_LLM to TraceSource.LLM, FixtureVariant.ALL_TEMPLATE to TraceSource.TEMPLATE)) {
            assertTrue("$v", d(v).lines.all { it.present && it.trace.size >= 2 && it.traceSource == src })
        }
        assertTrue(d(FixtureVariant.ALL_NONE).lines.all { it.traceSource == TraceSource.NONE && it.trace.isEmpty() })
        assertTrue(d(FixtureVariant.PRESENT_NOT_DRAWN).lines.any { it.present && it.traceSource == TraceSource.NONE && it.trace.isEmpty() })
        val long = d(FixtureVariant.LONG_TEXT)
        assertTrue(long.palm!!.summary.length >= 600)
        assertTrue(long.lines.all { it.interpretation.length >= 600 })
        assertEquals(en, d(FixtureVariant.MIXED))
        assertEquals(hi, d(FixtureVariant.HINDI))
        val invalids = FixtureVariant.entries.filter { it.code.startsWith("invalid_") }.map { d(it) }
        assertEquals(7, invalids.size)
        assertTrue(invalids.all { !it.verdict.ok })
        assertEquals(InvalidReason.entries.toSet(), invalids.map { it.verdict.reason }.toSet())
        assertFalse(d(FixtureVariant.ALL_LLM).lines.single { it.type == LineType.FATE }.trace.isEmpty())
    }
}
