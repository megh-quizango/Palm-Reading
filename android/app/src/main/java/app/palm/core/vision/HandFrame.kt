package app.palm.core.vision

import app.palm.contract.HandSide

/** One landmark: x, y normalised to the upright image, z relative to the wrist on x's scale. */
data class P3(val x: Float, val y: Float, val z: Float)

/** One detected hand. [handedness] is the raw model label, before any calibration swap. */
data class HandFrame(
    val points: List<P3>,
    val handedness: HandSide?,
    val handednessScore: Float,
    val width: Int,
    val height: Int,
    val timestampMs: Long,
)

enum class GuidanceState { NoHand, BackOfHand, ClosedHand, TooFar, Moving, PalmDetected }
