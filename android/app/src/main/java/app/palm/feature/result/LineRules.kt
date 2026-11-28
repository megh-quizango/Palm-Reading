package app.palm.feature.result

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import app.palm.R
import app.palm.contract.LineType
import app.palm.contract.PalmLineDto
import app.palm.contract.TraceSource
import app.palm.core.designsystem.NocturneColors

/** A line is drawn iff it has a real trace of at least 2 points (D-09, RESEARCH Pattern 3). */
fun PalmLineDto.isDrawn(): Boolean = traceSource != TraceSource.NONE && trace.size >= 2 && present

/** Reserved palm colours: overlay strokes, legend and card dots only. */
fun lineColor(type: LineType): Color = when (type) {
    LineType.HEART -> NocturneColors.PalmHeart
    LineType.HEAD -> NocturneColors.PalmHead
    LineType.LIFE -> NocturneColors.PalmLife
    LineType.FATE -> NocturneColors.PalmFate
    LineType.SUN -> NocturneColors.PalmSun
}

@StringRes
fun lineNameRes(type: LineType): Int = when (type) {
    LineType.HEART -> R.string.result_line_heart
    LineType.HEAD -> R.string.result_line_head
    LineType.LIFE -> R.string.result_line_life
    LineType.FATE -> R.string.result_line_fate
    LineType.SUN -> R.string.result_line_sun
}

@StringRes
fun lineFullNameRes(type: LineType): Int = when (type) {
    LineType.HEART -> R.string.result_line_heart_full
    LineType.HEAD -> R.string.result_line_head_full
    LineType.LIFE -> R.string.result_line_life_full
    LineType.FATE -> R.string.result_line_fate_full
    LineType.SUN -> R.string.result_line_sun_full
}

/** Drawn lines only, in contract order (heart, head, life, fate, sun). */
fun drawnLines(lines: List<PalmLineDto>): List<PalmLineDto> = lines.filter { it.isDrawn() }.sortedBy { it.type.ordinal }
