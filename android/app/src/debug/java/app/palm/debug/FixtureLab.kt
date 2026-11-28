package app.palm.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import app.palm.contract.HandSide
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneTopBar
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.ScanResultHolder
import app.palm.core.vision.P3
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Debug-only lab (D-05): puts a generated palm photo into ScanResultHolder and opens ScanReady, no camera needed. */
@Serializable data object FixtureLab : NavKey

private const val LAB_W = 960
private const val LAB_H = 1280

@HiltViewModel
class FixtureLabViewModel @Inject constructor(private val holder: ScanResultHolder) : ViewModel() {
    private var busy = false

    fun openScanReady(onReady: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    holder.put(labPhoto(), LAB_W, LAB_H, HandSide.RIGHT, List(21) { P3(0.5f, 0.5f, 0f) }, PhotoOrigin.GALLERY)
                }
                onReady()
            } finally {
                busy = false
            }
        }
    }
}

/** A 3:4 light-skin palm on a vertical gradient, JPEG 85. */
private fun labPhoto(): ByteArray {
    val bitmap = Bitmap.createBitmap(LAB_W, LAB_H, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val background = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, LAB_H.toFloat(), 0xFF3A3550.toInt(), 0xFF12101C.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, LAB_W.toFloat(), LAB_H.toFloat(), background)
        val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8C9B0.toInt() }
        canvas.drawRoundRect(RectF(210f, 520f, 750f, 1180f), 140f, 140f, skin)
        // Four fingers above the palm, the thumb on the left
        val fingers = listOf(RectF(230f, 140f, 340f, 600f), RectF(355f, 80f, 465f, 580f), RectF(480f, 100f, 590f, 580f), RectF(605f, 190f, 705f, 600f))
        fingers.forEach { canvas.drawRoundRect(it, 55f, 55f, skin) }
        canvas.save()
        canvas.rotate(-35f, 200f, 860f)
        canvas.drawRoundRect(RectF(60f, 760f, 330f, 880f), 60f, 60f, skin)
        canvas.restore()
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    } finally {
        bitmap.recycle()
    }
}

@Composable
fun FixtureLabScreen(onBack: () -> Unit, onOpenScanReady: () -> Unit, viewModel: FixtureLabViewModel = hiltViewModel()) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = NocturneSpace.S16),
    ) {
        NocturneTopBar("Fixture lab", onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = NocturneSpace.S16),
            verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
        ) {
            NocturneButton("Open ScanReady", onClick = { viewModel.openScanReady(onOpenScanReady) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
