package app.palm.core.vision

import app.palm.contract.HandSide

data class StableGuidance(val shown: GuidanceState, val captureEnabled: Boolean, val wrongHand: HandSide?)

/**
 * What the chip shows (D-08, research Pattern 3): a raw state replaces the shown one only after it
 * persisted dwellMs; while PalmDetected is shown, NoHand needs max(dwellMs, dropoutMs); capture
 * opens once PalmDetected has been shown holdMs. The wrong-hand hint has its own dwell.
 * Uses the frame timestamps passed in, never a clock.
 */
class GuidanceStabilizer(private val config: GuidanceConfig = GuidanceConfig.Default) {
    private var shown = GuidanceState.NoHand
    private var shownSince = 0L
    private var candidate: GuidanceState? = null
    private var candidateSince = 0L

    private var hint: HandSide? = null
    private var hintCandidate: HandSide? = null
    private var hintPending = false
    private var hintSince = 0L

    fun update(raw: GuidanceState, wrongHand: HandSide?, timestampMs: Long): StableGuidance {
        if (raw == shown) {
            candidate = null
        } else {
            if (raw != candidate) {
                candidate = raw
                candidateSince = timestampMs
            }
            val need = if (shown == GuidanceState.PalmDetected && raw == GuidanceState.NoHand) {
                maxOf(config.dwellMs, config.dropoutMs)
            } else {
                config.dwellMs
            }
            if (timestampMs - candidateSince >= need) {
                shown = raw
                shownSince = timestampMs
                candidate = null
            }
        }

        if (wrongHand == hint) {
            hintPending = false
        } else {
            if (!hintPending || wrongHand != hintCandidate) {
                hintPending = true
                hintCandidate = wrongHand
                hintSince = timestampMs
            }
            if (timestampMs - hintSince >= config.dwellMs) {
                hint = wrongHand
                hintPending = false
            }
        }

        val capture = shown == GuidanceState.PalmDetected && timestampMs - shownSince >= config.holdMs
        return StableGuidance(shown, capture, hint)
    }
}
