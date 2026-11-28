package app.palm.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle
import app.palm.core.designsystem.components.NocturneChoiceTile
import app.palm.core.designsystem.components.NocturneDialog
import app.palm.core.designsystem.components.NocturneTopBar
import app.palm.core.photo.PhotoOrigin
import app.palm.core.photo.ScanResultHolder
import app.palm.core.reading.FixtureVariant
import app.palm.core.reading.ReadingStore
import app.palm.core.vision.P3
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Debug-only lab (D-05, D-06): a generated palm photo at a chosen aspect and skin tone, a fixture variant, no camera. */
@Serializable data object FixtureLab : NavKey

enum class LabAspect(val label: String, val w: Int, val h: Int) {
    R3_4("3:4", 960, 1280),
    R1_1("1:1", 1080, 1080),
    R9_16("9:16", 720, 1280),
}

/** UI-SPEC Color "Overlay halo" skin tones. */
enum class LabSkin(val label: String, val argb: Int) {
    LIGHT("Light", 0xFFE8C9B0.toInt()),
    DARK("Dark", 0xFF5A4650.toInt()),
}

@HiltViewModel
class FixtureLabViewModel @Inject constructor(
    private val holder: ScanResultHolder,
    private val store: ReadingStore,
) : ViewModel() {
    var aspect by mutableStateOf(LabAspect.R3_4)
    var skin by mutableStateOf(LabSkin.LIGHT)
    var variant by mutableStateOf(FixtureLabChoice.variant ?: FixtureVariant.MIXED)
        private set
    private var busy = false

    /** Applies at once, so the next real scan reads this variant too (D-05). */
    fun choose(v: FixtureVariant) {
        variant = v
        FixtureLabChoice.variant = v
    }

    fun openScanReady(onReady: () -> Unit) {
        if (busy) return
        busy = true
        val a = aspect
        val s = skin
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    holder.put(labPhoto(a, s), a.w, a.h, HandSide.RIGHT, List(21) { P3(0.5f, 0.5f, 0f) }, PhotoOrigin.GALLERY)
                }
                onReady()
            } finally {
                busy = false
            }
        }
    }

    /** [onDone] runs once every row and photo is gone, so the dialog closes only after the sweep. */
    fun deleteAll(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                store.deleteAll()
            } finally {
                onDone()
            }
        }
    }
}

/** A palm silhouette on a vertical gradient, JPEG 85; the hand is drawn on a 960 x 1280 grid scaled to fit and centred. */
private fun labPhoto(aspect: LabAspect, skinTone: LabSkin): ByteArray {
    val w = aspect.w
    val h = aspect.h
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val background = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF3A3550.toInt(), 0xFF12101C.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), background)
        val scale = minOf(w / 960f, h / 1280f)
        canvas.translate((w - 960f * scale) / 2f, (h - 1280f * scale) / 2f)
        canvas.scale(scale, scale)
        val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinTone.argb }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixtureLabScreen(onBack: () -> Unit, onOpenScanReady: () -> Unit, viewModel: FixtureLabViewModel = hiltViewModel()) {
    val type = LocalNocturne.current.type
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = NocturneSpace.S16),
    ) {
        NocturneTopBar("Fixture lab", onBack = onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = NocturneSpace.S16),
            verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
        ) {
            Text("Photo aspect", style = type.labelM, color = NocturneColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                LabAspect.entries.forEach { a ->
                    NocturneChoiceTile(a.label, viewModel.aspect == a, { viewModel.aspect = a }, Modifier.weight(1f))
                }
            }
            Text("Skin", style = type.labelM, color = NocturneColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                LabSkin.entries.forEach { s ->
                    NocturneChoiceTile(s.label, viewModel.skin == s, { viewModel.skin = s }, Modifier.weight(1f))
                }
            }
            Text("Variant", style = type.labelM, color = NocturneColors.TextPrimary)
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
                verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8),
            ) {
                FixtureVariant.entries.forEach { v ->
                    NocturneChoiceTile("${v.label} (${v.code})", viewModel.variant == v, { viewModel.choose(v) }, Modifier.weight(1f))
                }
            }
        }
        // Pinned outside the scroll: uiautomator lists only on-screen nodes, so scripts can always reach both buttons
        Column(Modifier.padding(vertical = NocturneSpace.S16), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
            NocturneButton("Open ScanReady", onClick = { viewModel.openScanReady(onOpenScanReady) }, modifier = Modifier.fillMaxWidth())
            NocturneButton(
                "Delete all readings",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                style = NocturneButtonStyle.Ghost,
            )
        }
    }
    if (confirmDelete) {
        NocturneDialog(
            title = "Delete all readings?",
            body = "Removes every saved reading and its photo on this phone.",
            confirm = "Delete" to { viewModel.deleteAll { confirmDelete = false } },
            dismiss = "Cancel" to { confirmDelete = false },
            onDismissRequest = { confirmDelete = false },
        )
    }
}
