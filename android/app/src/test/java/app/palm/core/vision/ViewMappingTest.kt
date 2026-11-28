package app.palm.core.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewMappingTest {
    private val phone = FillCenterMapping(480, 640, 1080, 2400)

    @Test fun portraitFrameOnA20by9ScreenCropsTheSides() {
        assertEquals(3.75f, phone.scale, 1e-6f)
        assertEquals(VisibleRect(96f, 0f, 288f, 640f), phone.visibleRect())
    }

    @Test fun sameSizeViewSeesTheFullFrame() =
        assertEquals(VisibleRect(0f, 0f, 480f, 640f), FillCenterMapping(480, 640, 480, 640).visibleRect())

    @Test fun imageCentreMapsToViewCentreAndBack() {
        val (vx, vy) = phone.imageToView(240f, 320f)
        assertEquals(540f, vx, 0.01f)
        assertEquals(1200f, vy, 0.01f)
        val (ix, iy) = phone.viewToImage(vx, vy)
        assertEquals(240f, ix, 0.01f)
        assertEquals(320f, iy, 0.01f)
    }
}
