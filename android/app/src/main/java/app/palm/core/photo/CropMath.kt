package app.palm.core.photo

import app.palm.core.vision.P3

/** Pixel rectangle in the upright image, right/bottom exclusive. */
data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

const val CROP_MARGIN = 0.25f // around the landmark box; joints do not reach the palm edges or the heel
const val MAX_EDGE_PX = 1280
const val DECODE_EDGE_PX = 3072

/** Landmark bounding box grown by [margin] of its size on each side, clamped to the image. */
fun cropFor(points: List<P3>, width: Int, height: Int, margin: Float = CROP_MARGIN): CropRect {
    val xs = points.map { it.x * width }
    val ys = points.map { it.y * height }
    val bw = xs.max() - xs.min()
    val bh = ys.max() - ys.min()
    val left = (xs.min() - margin * bw).toInt().coerceIn(0, width - 1)
    val top = (ys.min() - margin * bh).toInt().coerceIn(0, height - 1)
    val right = (xs.max() + margin * bw).toInt().coerceIn(left + 1, width)
    val bottom = (ys.max() + margin * bh).toInt().coerceIn(top + 1, height)
    return CropRect(left, top, right, bottom)
}

/** A landmark normalised to the full image, re-normalised to [crop]; z shares x's scale. */
fun remapToCrop(p: P3, width: Int, height: Int, crop: CropRect): P3 =
    P3((p.x * width - crop.left) / crop.width, (p.y * height - crop.top) / crop.height, p.z * width / crop.width)

/** Long edge <= [maxEdge], aspect kept, never upscales. */
fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE_PX): Pair<Int, Int> {
    val long = maxOf(width, height)
    if (long <= maxEdge) return width to height
    val scale = maxEdge.toFloat() / long
    return maxOf(1, Math.round(width * scale)) to maxOf(1, Math.round(height * scale))
}

/** Smallest power of two that brings the long edge to <= [maxEdge]. */
fun sampleSizeFor(width: Int, height: Int, maxEdge: Int = DECODE_EDGE_PX): Int {
    var n = 1
    while (maxOf(width, height) / n > maxEdge) n *= 2
    return n
}

/** The backend's own rule: exactly 21 finite points with x and y in -0.5..1.5. */
fun landmarksValid(points: List<P3>): Boolean =
    points.size == 21 && points.all { p ->
        p.x.isFinite() && p.y.isFinite() && p.z.isFinite() && p.x in -0.5f..1.5f && p.y in -0.5f..1.5f
    }
