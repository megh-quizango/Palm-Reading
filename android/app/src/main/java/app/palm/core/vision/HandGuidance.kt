package app.palm.core.vision

import app.palm.contract.HandSide
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Every guidance threshold in one place (D-07, D-08). Starting values from research Pattern 2/3;
 * 04-05 calibrates them on the phone. tools/gen_landmark_fixtures.py mirrors the band values.
 */
data class GuidanceConfig(
    val swapHandedness: Boolean = false,
    val rightPalmChirality: Int = -1,
    val minHandednessScore: Float = 0.8f,
    val wrongHandScore: Float = 0.9f,
    val curlRatio: Float = 1.5f,
    val minCurledFingers: Int = 3,
    val tooFarEnter: Float = 0.40f,
    val tooFarExit: Float = 0.44f,
    val moveWindowMs: Long = 300,
    val moveEnter: Float = 0.12f,
    val moveExit: Float = 0.08f,
    val dwellMs: Long = 250,
    val holdMs: Long = 500,
    val dropoutMs: Long = 300,
) {
    companion object {
        val Default = GuidanceConfig()
    }
}

/** Part of the analysis image visible on screen, in image pixels. */
data class VisibleRect(val left: Float, val top: Float, val width: Float, val height: Float)

data class GuidanceReading(
    val raw: GuidanceState,
    val wrongHand: HandSide?,
    val identity: HandSide?,
    val chirality: Int,
    val curledFingers: Int,
    val handFraction: Float,
    val motion: Float,
    val palmCenter: P3?,
)

private val Fingers = listOf(5 to 8, 9 to 12, 13 to 16, 17 to 20) // (mcp, tip): index, middle, ring, pinky
private val PalmPoints = intArrayOf(0, 5, 9, 13, 17)

/**
 * Raw per-frame guidance state (D-07). All distances in pixels (research Pitfall 4).
 * Chirality = sign(cross(p5 - p0, p17 - p0)) with y down; identity is the corrected model label when
 * confident, else the chosen hand (a right palm and the back of a left hand are 2D mirror images,
 * Pitfall 1). Stateful: keeps the motion window and the TooFar / Moving hysteresis.
 */
class HandGuidance(private val chosen: HandSide, private val config: GuidanceConfig = GuidanceConfig.Default) {
    private val centres = ArrayDeque<Triple<Long, Float, Float>>() // (t, x px, y px)
    private var far = false
    private var moving = false

    fun read(frame: HandFrame?, visible: VisibleRect? = null): GuidanceReading {
        if (frame == null || frame.points.size < 21) {
            centres.clear()
            far = false
            moving = false
            return GuidanceReading(GuidanceState.NoHand, null, null, 0, 0, 0f, 0f, null)
        }
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val xs = FloatArray(21) { frame.points[it].x * w }
        val ys = FloatArray(21) { frame.points[it].y * h }
        fun dist(a: Int, b: Int) = hypot(xs[a] - xs[b], ys[a] - ys[b])

        val label = frame.handedness?.let { if (config.swapHandedness) it.other() else it }
        val identity = if (label != null && frame.handednessScore >= config.minHandednessScore) label else chosen
        val cross = (xs[5] - xs[0]) * (ys[17] - ys[0]) - (ys[5] - ys[0]) * (xs[17] - xs[0])
        val chirality = sign(cross).toInt()
        val palmChirality = if (identity == HandSide.RIGHT) config.rightPalmChirality else -config.rightPalmChirality
        val curled = Fingers.count { (mcp, tip) -> dist(tip, 0) < config.curlRatio * dist(mcp, 0) }

        val side = if (visible != null) min(visible.width, visible.height) else min(w, h)
        val fraction = max(xs.max() - xs.min(), ys.max() - ys.min()) / side
        far = if (far) fraction < config.tooFarExit else fraction < config.tooFarEnter

        val cx = PalmPoints.map { xs[it] }.average().toFloat()
        val cy = PalmPoints.map { ys[it] }.average().toFloat()
        val t = frame.timestampMs
        while (centres.isNotEmpty() && centres.first().first < t - config.moveWindowMs) centres.removeFirst()
        val handSize = dist(9, 0).coerceAtLeast(1f)
        val motion = (centres.maxOfOrNull { hypot(it.second - cx, it.third - cy) } ?: 0f) / handSize
        centres.addLast(Triple(t, cx, cy))
        moving = if (moving) motion > config.moveExit else motion > config.moveEnter

        val raw = when {
            far -> GuidanceState.TooFar
            curled >= config.minCurledFingers -> GuidanceState.ClosedHand
            chirality != palmChirality -> GuidanceState.BackOfHand
            moving -> GuidanceState.Moving
            else -> GuidanceState.PalmDetected
        }
        val wrongHand = label?.takeIf { frame.handednessScore >= config.wrongHandScore && it != chosen }
        val centre = P3(cx / w, cy / h, PalmPoints.map { frame.points[it].z }.average().toFloat())
        return GuidanceReading(raw, wrongHand, identity, chirality, curled, fraction, motion, centre)
    }
}

private fun HandSide.other() = if (this == HandSide.LEFT) HandSide.RIGHT else HandSide.LEFT
