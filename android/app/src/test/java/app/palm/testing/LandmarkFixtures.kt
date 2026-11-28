package app.palm.testing

import app.palm.contract.HandSide
import app.palm.core.vision.FillCenterMapping
import app.palm.core.vision.GuidanceState
import app.palm.core.vision.HandFrame
import app.palm.core.vision.P3
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FixtureFrame(val t: Long, val handedness: String?, val score: Float, val points: List<List<Float>>)

/** One file written by tools/gen_landmark_fixtures.py (or the 04-05 debug recorder under recorded/). */
@Serializable
data class LandmarkFixture(
    val label: String,
    val chosen: String,
    val expected: GuidanceState,
    val width: Int,
    val height: Int,
    val viewWidth: Int,
    val viewHeight: Int,
    val settleFrames: Int = 0,
    val frames: List<FixtureFrame>,
) {
    val hand get() = HandSide.valueOf(chosen) // HandSide's @SerialName is lowercase; fixtures use the enum name
    val mapping get() = FillCenterMapping(width, height, viewWidth, viewHeight)

    fun handFrames(): List<HandFrame?> = frames.map { f ->
        if (f.points.isEmpty()) {
            null
        } else {
            HandFrame(
                points = f.points.map { P3(it[0], it[1], it[2]) },
                handedness = when (f.handedness) { "Left" -> HandSide.LEFT; "Right" -> HandSide.RIGHT; else -> null },
                handednessScore = f.score,
                width = width,
                height = height,
                timestampMs = f.t,
            )
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

object LandmarkFixtures {
    fun load(name: String): LandmarkFixture {
        val url = requireNotNull(javaClass.getResource("/landmarks/$name.json")) { "fixture $name missing" }
        return json.decodeFromString(url.readText())
    }

    /** Every *.json directly in the classpath directory [dir], or null when it does not exist. */
    fun all(dir: String = "/landmarks/"): List<LandmarkFixture>? {
        val url = javaClass.getResource(dir) ?: return null
        return File(url.toURI()).listFiles { f -> f.extension == "json" }!!.sorted()
            .map { json.decodeFromString<LandmarkFixture>(it.readText()) }
    }
}
