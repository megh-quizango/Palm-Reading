package app.palm.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// PalmReading v1 (contracts/palm_reading.v1.schema.json). No default values: a missing key fails a strict decode.

@Serializable
data class PalmReadingDto(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("reading_id") val readingId: String,
    @SerialName("hand_side") val handSide: HandSide,
    @SerialName("meta") val meta: MetaDto,
    @SerialName("image_size") val imageSize: ImageSizeDto,
    @SerialName("verdict") val verdict: VerdictDto,
    @SerialName("lines") val lines: List<PalmLineDto>,
    @SerialName("palm") val palm: PalmSummaryDto?,
    @SerialName("love") val love: LoveDto?,
    @SerialName("career") val career: CareerDto?,
)

@Serializable
data class MetaDto(
    @SerialName("model") val model: String,
    @SerialName("prompt_version") val promptVersion: String,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("locale") val locale: String,
)

@Serializable
data class ImageSizeDto(
    @SerialName("w") val w: Int,
    @SerialName("h") val h: Int,
)

@Serializable
data class VerdictDto(
    @SerialName("ok") val ok: Boolean,
    @SerialName("reason") val reason: InvalidReason?,
)

@Serializable
data class PalmLineDto(
    @SerialName("type") val type: LineType,
    @SerialName("label") val label: String,
    @SerialName("present") val present: Boolean,
    @SerialName("trace") val trace: List<List<Double>>,
    @SerialName("trace_source") val traceSource: TraceSource,
    @SerialName("interpretation") val interpretation: String,
)

@Serializable
data class PalmSummaryDto(
    @SerialName("summary") val summary: String,
    @SerialName("traits") val traits: List<String>,
)

@Serializable
data class LoveDto(
    @SerialName("how_you_love") val howYouLove: String,
    @SerialName("ideal_partner") val idealPartner: String,
    @SerialName("challenges") val challenges: String,
)

@Serializable
data class CareerDto(
    @SerialName("strengths") val strengths: String,
    @SerialName("challenges") val challenges: String,
    @SerialName("ideal_industries") val idealIndustries: List<String>,
)

@Serializable
enum class HandSide { @SerialName("left") LEFT, @SerialName("right") RIGHT }

@Serializable
enum class LineType {
    @SerialName("heart") HEART,
    @SerialName("head") HEAD,
    @SerialName("life") LIFE,
    @SerialName("fate") FATE,
    @SerialName("sun") SUN,
}

@Serializable
enum class TraceSource { @SerialName("llm") LLM, @SerialName("template") TEMPLATE, @SerialName("none") NONE }

@Serializable
enum class InvalidReason {
    @SerialName("blurry_or_dark") BLURRY_OR_DARK,
    @SerialName("too_far") TOO_FAR,
    @SerialName("jewelry") JEWELRY,
    @SerialName("bent") BENT,
    @SerialName("fingers_tight") FINGERS_TIGHT,
    @SerialName("incomplete") INCOMPLETE,
    @SerialName("no_palm_lines") NO_PALM_LINES,
}
