package app.palm.debug

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneChip
import app.palm.core.vision.GuidanceState
import app.palm.feature.scan.ScanDebugSample
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Pose chips of the recorder and the state each recording must classify to (D-09). */
internal enum class Pose(val expected: GuidanceState) {
    palm(GuidanceState.PalmDetected),
    back(GuidanceState.BackOfHand),
    fist(GuidanceState.ClosedHand),
    far(GuidanceState.TooFar),
    moving(GuidanceState.Moving),
}

@Serializable
internal data class RecordedFrame(val t: Long, val handedness: String?, val score: Float, val points: List<List<Float>>)

/** Same shape as tools/gen_landmark_fixtures.py writes, so HandGuidanceTest reads it from landmarks/recorded/. */
@Serializable
internal data class RecordedFixture(
    val label: String,
    val chosen: String,
    val expected: GuidanceState,
    val width: Int,
    val height: Int,
    val viewWidth: Int,
    val viewHeight: Int,
    val settleFrames: Int = 0,
    val frames: List<RecordedFrame>,
)

/** Writes dir/HAND_POSE.json; pulled with `adb exec-out run-as app.palm.dev.debug cat files/landmarks/NAME`. Never logs. */
internal class LandmarkRecorder(private val dir: File) {
    private val json = Json { prettyPrint = true }

    /** Returns the file name, or null when no sample had a hand (nothing to calibrate from). */
    suspend fun write(hand: HandSide, pose: Pose, samples: List<Pair<Long, ScanDebugSample>>): String? {
        val sized = samples.firstOrNull { it.second.frame != null }?.second ?: return null
        val t0 = samples.first().first
        val label = "${hand.name.lowercase(Locale.ROOT)}_${pose.name}"
        val fixture = RecordedFixture(
            label = label,
            chosen = hand.name,
            expected = pose.expected,
            width = sized.frame!!.width,
            height = sized.frame.height,
            viewWidth = sized.viewWidth,
            viewHeight = sized.viewHeight,
            frames = samples.map { (t, s) ->
                val f = s.frame
                RecordedFrame(
                    t = t - t0,
                    handedness = when (f?.handedness) { HandSide.LEFT -> "Left"; HandSide.RIGHT -> "Right"; null -> null },
                    score = f?.handednessScore ?: 0f,
                    points = f?.points?.map { listOf(it.x, it.y, it.z) } ?: emptyList(),
                )
            },
        )
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            File(dir, "$label.json").writeText(json.encodeToString(RecordedFixture.serializer(), fixture))
        }
        return label
    }
}

/** Debug-only HUD on the scanner (04-05): raw guidance numbers and the 3 s landmark recorder. */
@Composable
fun ScanDebugHud(samples: StateFlow<ScanDebugSample?>, hand: HandSide, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
        NocturneChip("HUD", Modifier.clickable { expanded = !expanded }, textColor = NocturneColors.TextPrimary)
        if (expanded) HudPanel(samples, hand)
    }
}

@Composable
private fun HudPanel(samples: StateFlow<ScanDebugSample?>, hand: HandSide) {
    val sample by samples.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recorder = remember { LandmarkRecorder(File(context.filesDir, "landmarks")) }
    val scope = rememberCoroutineScope()
    var pose by rememberSaveable { mutableStateOf(Pose.palm) }
    var status by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var fps by remember { mutableFloatStateOf(0f) }

    // Analysed frames over the last second
    LaunchedEffect(samples) {
        val times = ArrayDeque<Long>()
        samples.collect { s ->
            val now = s?.timestampMs ?: SystemClock.uptimeMillis()
            times.addLast(now)
            while (times.isNotEmpty() && now - times.first() > 1000) times.removeFirst()
            fps = times.size.toFloat()
        }
    }

    val type = LocalNocturne.current.type
    Column(
        Modifier
            .widthIn(max = 220.dp)
            .background(NocturneColors.SurfaceScrim.copy(alpha = 0.72f), RoundedCornerShape(NocturneRadius.Md))
            .padding(NocturneSpace.S8),
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4),
    ) {
        val r = sample?.reading
        val f = sample?.frame
        val lines = listOf(
            "raw ${r?.raw ?: "-"}",
            "label ${f?.handedness ?: "-"} ${f?.handednessScore?.let { "%.2f".format(Locale.ROOT, it) } ?: ""}",
            "identity ${r?.identity ?: "-"}  wrong ${r?.wrongHand ?: "-"}",
            "chirality ${r?.chirality ?: "-"}  curled ${r?.curledFingers ?: "-"}",
            "fraction ${r?.handFraction?.let { "%.3f".format(Locale.ROOT, it) } ?: "-"}",
            "motion ${r?.motion?.let { "%.3f".format(Locale.ROOT, it) } ?: "-"}",
            "fps ${"%.0f".format(Locale.ROOT, fps)}",
        )
        lines.forEach { Text(it, style = type.caption, color = NocturneColors.TextPrimary) }
        Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
            Pose.entries.forEach { p ->
                Text(
                    p.name,
                    Modifier
                        .background(
                            if (p == pose) NocturneColors.FocusRing.copy(alpha = 0.5f) else NocturneColors.SurfaceScrim,
                            RoundedCornerShape(NocturneRadius.Sm),
                        )
                        .clickable(enabled = !recording) { pose = p }
                        .padding(horizontal = NocturneSpace.S4, vertical = NocturneSpace.S2),
                    style = type.caption,
                    color = NocturneColors.TextPrimary,
                )
            }
        }
        Text(
            if (recording) "Recording…" else "Record 3 s",
            Modifier
                .background(NocturneColors.FocusRing.copy(alpha = 0.5f), RoundedCornerShape(NocturneRadius.Sm))
                .clickable(enabled = !recording) {
                    recording = true
                    status = null
                    scope.launch {
                        val picked = pose
                        val got = mutableListOf<Pair<Long, ScanDebugSample>>()
                        withTimeoutOrNull(3_000) {
                            samples.collect { s ->
                                if (s != null) got += s.timestampMs to s
                            }
                        }
                        val name = recorder.write(hand, picked, got)
                        status = if (name == null) "no hand seen, nothing saved" else "saved $name (${got.size} frames)"
                        recording = false
                    }
                }
                .padding(horizontal = NocturneSpace.S8, vertical = NocturneSpace.S4),
            style = type.labelM,
            color = NocturneColors.TextPrimary,
        )
        status?.let { Text(it, style = type.caption, color = NocturneColors.TextPrimary) }
    }
}
