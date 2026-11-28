package app.palm.core.reading

import app.palm.contract.LineType
import app.palm.contract.PalmReadingDto
import app.palm.contract.TraceSource
import kotlinx.serialization.json.Json

/** The only contract encoder and decoder in the app: strict default Json (unknown or missing keys fail). */
val ContractJson: Json = Json

/** The schema invariants the JSON Schema states in prose (D-04), in one place. */
object ReadingRules {
    private val required = listOf(LineType.HEART, LineType.HEAD, LineType.LIFE)

    /** Empty = valid. Entries are rule names plus a line type, never reading text (D-18). */
    fun check(r: PalmReadingDto): List<String> = buildList {
        if (r.schemaVersion != 1) add("schema_version")
        if (r.verdict.ok) {
            if (r.verdict.reason != null) add("ok with reason")
            if (r.palm == null || r.love == null || r.career == null) add("ok without sections")
            if (!r.lines.map { it.type }.containsAll(required)) add("ok without required lines")
        } else {
            if (r.verdict.reason == null) add("invalid without reason")
            if (r.lines.isNotEmpty()) add("invalid with lines")
            if (r.palm != null || r.love != null || r.career != null) add("invalid with sections")
        }
        if (r.lines.map { it.type }.toSet().size != r.lines.size) add("duplicate type")
        for (l in r.lines) {
            if ((l.traceSource == TraceSource.NONE) != l.trace.isEmpty()) add("${l.type} none iff empty")
            if (l.traceSource != TraceSource.NONE && l.trace.size < 2) add("${l.type} under 2 points")
            if (!l.present && l.trace.isNotEmpty()) add("${l.type} absent with trace")
            if (l.trace.any { p -> p.size != 2 || p.any { it !in 0.0..1.0 } }) add("${l.type} point")
        }
    }
}
