package app.palm.feature.result

// Pure overlay maths (D-09): no android.* import, so it is unit-testable on the JVM.

/** Where an image drawn with ContentScale.Fit and Alignment.Center lands inside its box. */
data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)

fun fitRect(srcW: Int, srcH: Int, dstW: Float, dstH: Float): FitRect {
    val s = minOf(dstW / srcW, dstH / srcH)
    val w = srcW * s
    val h = srcH * s
    return FitRect((dstW - w) / 2f, (dstH - h) / 2f, w, h)
}

/** A normalised trace point (clamped to 0..1) in box pixels. */
fun FitRect.map(x: Double, y: Double): Pair<Float, Float> =
    left + x.coerceIn(0.0, 1.0).toFloat() * width to top + y.coerceIn(0.0, 1.0).toFloat() * height

/** One cubic Bézier segment ending at ([x], [y]). */
data class Cubic(val c1x: Float, val c1y: Float, val c2x: Float, val c2y: Float, val x: Float, val y: Float)

/**
 * Uniform Catmull-Rom through every point as cubic Béziers (RESEARCH Pattern 2), endpoints duplicated:
 * segment i uses C1 = Pi + (Pi+1 - Pi-1) / 6 and C2 = Pi+1 - (Pi+2 - Pi) / 6. Two points give a straight segment.
 */
fun catmullRom(px: FloatArray, py: FloatArray): List<Cubic> {
    val n = px.size
    require(n >= 2 && py.size == n) { "need at least 2 points" }
    return (0 until n - 1).map { i ->
        val i0 = maxOf(i - 1, 0)
        val i3 = minOf(i + 2, n - 1)
        Cubic(
            px[i] + (px[i + 1] - px[i0]) / 6f, py[i] + (py[i + 1] - py[i0]) / 6f,
            px[i + 1] - (px[i3] - px[i]) / 6f, py[i + 1] - (py[i3] - py[i]) / 6f,
            px[i + 1], py[i + 1],
        )
    }
}

/** At most [max] points, sampled evenly and keeping the first and last (T-05-08: the schema has no maxItems). */
fun capPoints(trace: List<List<Double>>, max: Int = 64): List<List<Double>> {
    if (trace.size <= max) return trace
    return List(max) { i -> trace[Math.round(i.toDouble() * (trace.size - 1) / (max - 1)).toInt()] }
}

/** True when the aspect ratios aw/ah and bw/bh differ by more than [tolerance] (relative, symmetric). */
fun aspectMismatch(aw: Int, ah: Int, bw: Int, bh: Int, tolerance: Float = 0.02f): Boolean {
    val a = aw.toDouble() / ah
    val b = bw.toDouble() / bh
    return maxOf(a, b) / minOf(a, b) - 1.0 > tolerance
}
