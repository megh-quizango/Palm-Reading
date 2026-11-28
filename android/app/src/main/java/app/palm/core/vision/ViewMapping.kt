package app.palm.core.vision

/**
 * PreviewView FILL_CENTER between the analysis image and the view (research Pitfall 3):
 * the image is scaled by max(view / image) per axis and centred, so the off-screen part is cropped.
 */
data class FillCenterMapping(val imageWidth: Int, val imageHeight: Int, val viewWidth: Int, val viewHeight: Int) {
    val scale: Float = maxOf(viewWidth.toFloat() / imageWidth, viewHeight.toFloat() / imageHeight)

    fun visibleRect(): VisibleRect {
        val w = viewWidth / scale
        val h = viewHeight / scale
        return VisibleRect((imageWidth - w) / 2f, (imageHeight - h) / 2f, w, h)
    }

    fun imageToView(xPx: Float, yPx: Float): Pair<Float, Float> {
        val r = visibleRect()
        return (xPx - r.left) * scale to (yPx - r.top) * scale
    }

    fun viewToImage(xPx: Float, yPx: Float): Pair<Float, Float> {
        val r = visibleRect()
        return xPx / scale + r.left to yPx / scale + r.top
    }
}
