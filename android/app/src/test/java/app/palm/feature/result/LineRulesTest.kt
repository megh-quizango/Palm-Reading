package app.palm.feature.result

import app.palm.R
import app.palm.contract.LineType
import app.palm.contract.PalmLineDto
import app.palm.contract.TraceSource
import app.palm.core.designsystem.NocturneColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineRulesTest {
    private fun line(
        type: LineType = LineType.HEART,
        source: TraceSource = TraceSource.LLM,
        points: Int = 2,
        present: Boolean = true,
    ) = PalmLineDto(type, "", present, List(points) { listOf(0.1 * it, 0.5) }, source, "")

    @Test fun llmAndTemplateTracesAreDrawnAndApproximate() {
        for (l in listOf(line(source = TraceSource.LLM, points = 2), line(source = TraceSource.TEMPLATE, points = 5))) {
            assertTrue(l.isDrawn())
            assertEquals(Badge.APPROXIMATE, l.badge())
        }
        assertEquals(R.string.result_badge_approx, badgeRes(Badge.APPROXIMATE))
    }

    @Test fun undrawnLinesAreNotClearlyVisible() {
        val undrawn = listOf(
            line(source = TraceSource.NONE, points = 0),
            line(source = TraceSource.NONE, points = 0, present = true),
            line(source = TraceSource.NONE, points = 0, present = false),
            line(source = TraceSource.LLM, points = 1), // defensive: rules reject it, the renderer still must not draw it
        )
        for (l in undrawn) {
            assertFalse(l.isDrawn())
            assertEquals(Badge.NOT_VISIBLE, l.badge())
        }
        assertEquals(R.string.result_badge_not_visible, badgeRes(Badge.NOT_VISIBLE))
    }

    @Test fun drawnAndOrderedLinesFollowContractOrder() {
        val input = listOf(line(LineType.SUN), line(LineType.HEART), line(LineType.FATE, TraceSource.NONE, 0, false), line(LineType.LIFE))
        assertEquals(listOf(LineType.HEART, LineType.LIFE, LineType.SUN), drawnLines(input).map { it.type })
        assertEquals(listOf(LineType.HEART, LineType.LIFE, LineType.FATE, LineType.SUN), orderedLines(input).map { it.type })
        assertEquals(listOf(LineType.HEART, LineType.LIFE, LineType.SUN), orderedLines(input.filter { it.type != LineType.FATE }).map { it.type })
    }

    @Test fun lineColoursAreThePalmTokens() {
        assertEquals(NocturneColors.PalmHeart, lineColor(LineType.HEART))
        assertEquals(NocturneColors.PalmHead, lineColor(LineType.HEAD))
        assertEquals(NocturneColors.PalmLife, lineColor(LineType.LIFE))
        assertEquals(NocturneColors.PalmFate, lineColor(LineType.FATE))
        assertEquals(NocturneColors.PalmSun, lineColor(LineType.SUN))
    }
}
