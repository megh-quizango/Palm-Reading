package app.palm.feature.result

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.palm.R
import app.palm.contract.PalmLineDto
import app.palm.contract.PalmReadingDto
import app.palm.contract.TraceSource
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneTheme
import app.palm.core.reading.ContractJson
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** D-24: line colours land on the mapped trace points on 3:4, 1:1, 9:16 and a letterboxed box; nothing drawn for none. */
@RunWith(AndroidJUnit4::class)
class PalmOverlayTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val sample: PalmReadingDto by lazy {
        val text = compose.activity.assets.open("fixtures/palm_reading.en.json").use { it.readBytes().decodeToString() }
        ContractJson.decodeFromString<PalmReadingDto>(text)
    }

    private fun gray(w: Int, h: Int) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF808080.toInt()) }

    private class Scene(val bitmap: Bitmap, val lines: List<PalmLineDto>, val wDp: Float, val hDp: Float)

    private var scene by mutableStateOf<Scene?>(null)
    private var started = false

    /** Renders the photo static (revealKey null) in a [wDp] x [hDp] box and returns its pixels; setContent runs once per test. */
    private fun render(bitmap: Bitmap, lines: List<PalmLineDto>, wDp: Float, hDp: Float): PixelMap {
        compose.runOnIdle { scene = Scene(bitmap, lines, wDp, hDp) }
        if (!started) {
            started = true
            compose.setContent {
                NocturneTheme {
                    scene?.let { s ->
                        val image = remember(s) { s.bitmap.asImageBitmap() }
                        PalmPhoto(PhotoState.Ready(image), s.lines, Modifier.testTag(TAG).size(s.wDp.dp, s.hDp.dp))
                    }
                }
            }
        }
        compose.waitForIdle()
        return compose.onNodeWithTag(TAG).captureToImage().toPixelMap()
    }

    private fun Color.near(o: Color) =
        abs(red - o.red) * 255 <= TOL && abs(green - o.green) * 255 <= TOL && abs(blue - o.blue) * 255 <= TOL

    private fun PixelMap.hasNear(cx: Int, cy: Int, c: Color): Boolean {
        for (dx in -1..1) for (dy in -1..1) {
            val x = cx + dx
            val y = cy + dy
            if (x in 0 until width && y in 0 until height && this[x, y].near(c)) return true
        }
        return false
    }

    private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val vx = bx - ax
        val vy = by - ay
        val len2 = vx * vx + vy * vy
        val t = if (len2 == 0f) 0f else (((px - ax) * vx + (py - ay) * vy) / len2).coerceIn(0f, 1f)
        return hypot(px - (ax + t * vx), py - (ay + t * vy))
    }

    private fun assertLinesOnPoints(bitmap: Bitmap, wDp: Float, hDp: Float) {
        val pixels = render(bitmap, sample.lines, wDp, hDp)
        val rect = fitRect(bitmap.width, bitmap.height, pixels.width.toFloat(), pixels.height.toFloat())
        val skip = 6f * compose.density.density
        val drawn = drawnLines(sample.lines)
        val mapped = drawn.map { l -> l.trace.map { rect.map(it[0], it[1]) } }
        var checked = 0
        drawn.forEachIndexed { i, line ->
            for ((x, y) in mapped[i]) {
                val covered = mapped.drop(i + 1).any { later ->
                    later.zipWithNext().any { (a, b) -> distToSegment(x, y, a.first, a.second, b.first, b.second) < skip }
                }
                if (covered) continue
                assertTrue(
                    "${line.type} at (${x.roundToInt()}, ${y.roundToInt()}) on ${bitmap.width}x${bitmap.height} in ${wDp}x$hDp dp",
                    pixels.hasNear(x.roundToInt(), y.roundToInt(), lineColor(line.type)),
                )
                checked++
            }
        }
        assertTrue("checked $checked points", checked >= 12)
    }

    @Test fun linesLandOnTheTracePointsOnThreeAspects() {
        assertLinesOnPoints(gray(960, 1280), 360f, 480f)
        assertLinesOnPoints(gray(1080, 1080), 360f, 360f)
        assertLinesOnPoints(gray(720, 1280), 360f, 640f)
    }

    @Test fun linesLandOnTheTracePointsWhenLetterboxed() {
        val bitmap = gray(960, 1280)
        assertLinesOnPoints(bitmap, 360f, 300f)
        val pixels = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()
        val rect = fitRect(bitmap.width, bitmap.height, pixels.width.toFloat(), pixels.height.toFloat())
        val band = pixels[(rect.left - 4).roundToInt(), pixels.height / 2]
        assertTrue("letterbox band $band", band.near(NocturneColors.BgDeep))
    }

    @Test fun noneLinesDrawNothing() {
        val none = sample.lines.map { it.copy(trace = emptyList(), traceSource = TraceSource.NONE) }
        val pixels = render(gray(960, 1280), none, 360f, 480f)
        val palm = listOf(NocturneColors.PalmHeart, NocturneColors.PalmHead, NocturneColors.PalmLife, NocturneColors.PalmFate, NocturneColors.PalmSun)
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val p = pixels[x, y]
            assertTrue("palm colour at ($x, $y): $p", palm.none { p.near(it) })
        }
        assertEquals(compose.activity.getString(R.string.result_photo_cd_plain), description())
    }

    @Test fun descriptionNamesExactlyTheDrawnLines() {
        render(gray(960, 1280), sample.lines, 360f, 480f)
        val names = listOf(R.string.result_line_heart, R.string.result_line_head, R.string.result_line_life, R.string.result_line_sun)
            .joinToString(", ") { compose.activity.getString(it) }
        assertEquals(compose.activity.getString(R.string.result_photo_cd, names), description())
    }

    private fun description(): String =
        compose.onNodeWithTag(TAG).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()

    private companion object {
        const val TAG = "palm-photo"
        const val TOL = 12f
    }
}
