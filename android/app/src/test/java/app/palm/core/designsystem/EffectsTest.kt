package app.palm.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class EffectsTest {
    private fun assertLine(angle: Float, size: Size, from: Offset, to: Offset) {
        val (a, b) = gradientLine(angle, size)
        assertEquals("from.x @$angle", from.x, a.x, 0.001f)
        assertEquals("from.y @$angle", from.y, a.y, 0.001f)
        assertEquals("to.x @$angle", to.x, b.x, 0.001f)
        assertEquals("to.y @$angle", to.y, b.y, 0.001f)
    }

    @Test fun primary90RunsLeftToRight() =
        assertLine(NocturneGradientAngles.Primary, Size(100f, 50f), Offset(0f, 25f), Offset(100f, 25f))

    @Test fun sky180RunsTopToBottom() =
        assertLine(NocturneGradientAngles.Sky, Size(100f, 50f), Offset(50f, 0f), Offset(50f, 50f))

    @Test fun card135RunsTopLeftToBottomRight() =
        assertLine(NocturneGradientAngles.Card, Size(100f, 100f), Offset(0f, 0f), Offset(100f, 100f))
}
