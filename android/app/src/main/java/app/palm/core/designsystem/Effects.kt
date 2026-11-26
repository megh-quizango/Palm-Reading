package app.palm.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow // the drop-shadow type, not the text Shadow (Pitfall 13)
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gradient directions in CSS degrees (0 points up, clockwise), RESEARCH Open Question 3 (RESOLVED).
 * The tokens carry stops only; this is the one place angles are written.
 */
object NocturneGradientAngles {
    const val Primary = 90f // left to right
    const val Hero = 135f
    const val Sky = 180f // top to bottom
    const val Card = 135f // top-left to bottom-right
    const val FoilBorder = 135f
    const val HeroAurora = 135f
}

/** Outline width for ghost buttons, chips and the foil border; the tokens define no border width. */
val NocturneBorderWidth = 1.dp

/** The one conversion from a token glow to a Compose drop shadow (Figma blur = Compose radius, A10). */
fun Modifier.nocturneGlow(shadow: NocturneShadow, shape: Shape): Modifier = dropShadow(
    shape,
    Shadow(
        radius = shadow.blur,
        color = shadow.color,
        spread = shadow.spread,
        offset = DpOffset(shadow.offsetX, shadow.offsetY),
    ),
)

/** CSS linear-gradient line: through the centre along (sin a, -cos a), length |w sin a| + |h cos a|. */
internal fun gradientLine(angleDegrees: Float, size: Size): Pair<Offset, Offset> {
    val rad = Math.toRadians(angleDegrees.toDouble())
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
    val c = size.center
    return Offset(c.x - dx * half, c.y - dy * half) to Offset(c.x + dx * half, c.y + dy * half)
}

/** A token gradient ([NocturneGradients]) drawn at a [NocturneGradientAngles] angle on any size. */
fun nocturneBrush(stops: Array<Pair<Float, Color>>, angleDegrees: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val (from, to) = gradientLine(angleDegrees, size)
        return LinearGradientShader(from, to, stops.map { it.second }, stops.map { it.first })
    }
}
