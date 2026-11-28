package app.palm.core.reading

import app.palm.contract.InvalidReason
import app.palm.contract.LineType
import app.palm.contract.PalmReadingDto
import app.palm.contract.TraceSource

/** The fixture readings the debug lab can pick (D-06), plus a long-text backstop for the UI-SPEC font-scale checks. */
enum class FixtureVariant(val code: String, val label: String) {
    MIXED("mixed", "Valid · mixed sources"),
    ALL_LLM("all_llm", "Valid · all llm"),
    ALL_TEMPLATE("all_template", "Valid · all template"),
    ALL_NONE("all_none", "Valid · all lines none"),
    PRESENT_NOT_DRAWN("present_not_drawn", "Valid · present but not drawn"),
    HINDI("hindi", "Valid · Hindi"),
    LONG_TEXT("long_text", "Valid · long text"),
    INVALID_BLURRY_OR_DARK("invalid_blurry_or_dark", "Invalid · blurry_or_dark"),
    INVALID_TOO_FAR("invalid_too_far", "Invalid · too_far"),
    INVALID_JEWELRY("invalid_jewelry", "Invalid · jewelry"),
    INVALID_BENT("invalid_bent", "Invalid · bent"),
    INVALID_FINGERS_TIGHT("invalid_fingers_tight", "Invalid · fingers_tight"),
    INVALID_INCOMPLETE("invalid_incomplete", "Invalid · incomplete"),
    INVALID_NO_PALM_LINES("invalid_no_palm_lines", "Invalid · no_palm_lines"),
}

/** Drawn for a line whose fixture trace is empty when a variant forces every line present. */
private val FATE_TRACE = listOf(listOf(0.5, 0.9), listOf(0.5, 0.75), listOf(0.51, 0.6), listOf(0.52, 0.45))

private const val LONG_MIN = 600

private fun String.long(): String = generateSequence(this) { "$it $this" }.first { it.length >= LONG_MIN }

/** Replaces the "no clear line" text of a line a variant forces present, so the card matches the drawing. */
private fun presentText(locale: String) =
    if (locale == "hi") "यहाँ एक हल्की रेखा दिखती है, जिसे हस्तरेखा पढ़ने वाले अक्सर धीरे-धीरे बनती दिशा मानते हैं।"
    else "A faint line shows here, which palm readers often take as a direction that forms gradually."

private fun PalmReadingDto.allDrawn(source: TraceSource) = copy(
    lines = lines.map {
        it.copy(
            present = true,
            traceSource = source,
            trace = it.trace.ifEmpty { FATE_TRACE },
            interpretation = if (it.present) it.interpretation else presentText(meta.locale),
        )
    },
)

/** Pure: [app] is the app-language fixture, [hindi] and [invalid] the other two. */
fun FixtureVariant.derive(app: PalmReadingDto, hindi: PalmReadingDto, invalid: PalmReadingDto): PalmReadingDto = when (this) {
    FixtureVariant.MIXED -> app
    FixtureVariant.ALL_LLM -> app.allDrawn(TraceSource.LLM)
    FixtureVariant.ALL_TEMPLATE -> app.allDrawn(TraceSource.TEMPLATE)
    FixtureVariant.ALL_NONE -> app.copy(lines = app.lines.map { it.copy(trace = emptyList(), traceSource = TraceSource.NONE) })
    FixtureVariant.PRESENT_NOT_DRAWN -> app.copy(
        lines = app.lines.map { if (it.type == LineType.HEART) it.copy(present = true, trace = emptyList(), traceSource = TraceSource.NONE) else it },
    )
    FixtureVariant.HINDI -> hindi
    FixtureVariant.LONG_TEXT -> app.copy(
        palm = app.palm?.let { it.copy(summary = it.summary.long()) },
        lines = app.lines.map { it.copy(interpretation = it.interpretation.long()) },
    )
    FixtureVariant.INVALID_BLURRY_OR_DARK -> invalid.withReason(InvalidReason.BLURRY_OR_DARK)
    FixtureVariant.INVALID_TOO_FAR -> invalid.withReason(InvalidReason.TOO_FAR)
    FixtureVariant.INVALID_JEWELRY -> invalid.withReason(InvalidReason.JEWELRY)
    FixtureVariant.INVALID_BENT -> invalid.withReason(InvalidReason.BENT)
    FixtureVariant.INVALID_FINGERS_TIGHT -> invalid.withReason(InvalidReason.FINGERS_TIGHT)
    FixtureVariant.INVALID_INCOMPLETE -> invalid.withReason(InvalidReason.INCOMPLETE)
    FixtureVariant.INVALID_NO_PALM_LINES -> invalid.withReason(InvalidReason.NO_PALM_LINES)
}

private fun PalmReadingDto.withReason(reason: InvalidReason) = copy(verdict = verdict.copy(reason = reason))
