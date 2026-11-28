package app.palm.core.reading

import app.palm.contract.InvalidReason
import app.palm.contract.LineType
import app.palm.contract.PalmLineDto
import app.palm.contract.PalmReadingDto
import app.palm.contract.TraceSource
import app.palm.contract.VerdictDto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingRulesTest {
    private fun contract(name: String): PalmReadingDto {
        val dir = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
        return ContractJson.decodeFromString<PalmReadingDto>(File(dir, name).readText())
    }

    private val ok = contract("palm_reading.sample.json")

    private fun PalmReadingDto.editLine(type: LineType, edit: (PalmLineDto) -> PalmLineDto) =
        copy(lines = lines.map { if (it.type == type) edit(it) else it })

    @Test fun bothSamplesAreValid() {
        assertEquals(emptyList<String>(), ReadingRules.check(ok))
        assertEquals(emptyList<String>(), ReadingRules.check(contract("palm_reading.invalid_photo.sample.json")))
    }

    @Test fun eachViolationNamesItsRule() {
        val heart = ok.lines.first { it.type == LineType.HEART }
        val cases = listOf(
            ok.editLine(LineType.HEART) { it.copy(traceSource = TraceSource.NONE) } to "HEART none iff empty",
            ok.editLine(LineType.HEART) { it.copy(trace = it.trace.take(1)) } to "HEART under 2 points",
            ok.editLine(LineType.HEART) { it.copy(present = false) } to "HEART absent with trace",
            ok.editLine(LineType.HEART) { it.copy(trace = it.trace + listOf(listOf(1.2, 0.5))) } to "HEART point",
            ok.editLine(LineType.HEART) { it.copy(trace = it.trace + listOf(listOf(0.1, 0.2, 0.3))) } to "HEART point",
            ok.copy(lines = ok.lines + heart) to "duplicate type",
            ok.copy(lines = ok.lines.filter { it.type != LineType.HEART }) to "ok without required lines",
            ok.copy(verdict = VerdictDto(true, InvalidReason.BENT)) to "ok with reason",
            ok.copy(palm = null) to "ok without sections",
            ok.copy(verdict = VerdictDto(false, InvalidReason.BENT), palm = null, love = null, career = null) to "invalid with lines",
            ok.copy(schemaVersion = 2) to "schema_version",
        )
        for ((reading, rule) in cases) {
            val result = ReadingRules.check(reading)
            assertTrue("$rule not in $result", rule in result)
        }
    }

    @Test fun messagesNeverCarryReadingText() {
        val broken = ok.copy(schemaVersion = 2, lines = ok.lines + ok.lines.first(), palm = null)
            .editLine(LineType.HEAD) { it.copy(trace = listOf(listOf(2.0, 2.0))) }
        val texts = ok.lines.flatMap { listOf(it.label, it.interpretation) } + listOfNotNull(ok.palm?.summary)
        val result = ReadingRules.check(broken)
        assertTrue(result.isNotEmpty())
        for (message in result) for (text in texts) assertTrue(message, !message.contains(text))
    }
}
