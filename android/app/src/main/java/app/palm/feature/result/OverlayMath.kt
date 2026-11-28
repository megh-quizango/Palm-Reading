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
