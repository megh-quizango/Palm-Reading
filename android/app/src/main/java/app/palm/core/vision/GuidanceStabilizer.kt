package app.palm.core.vision

import app.palm.contract.HandSide

data class StableGuidance(val shown: GuidanceState, val captureEnabled: Boolean, val wrongHand: HandSide?)

/**
 * What the chip shows (D-08, research Pattern 3): shown changes to the latest raw state once raw has
 * differed from it for dwellMs; while PalmDetected is shown, NoHand needs max(dwellMs, dropoutMs); capture
 * opens once PalmDetected has been shown holdMs. The wrong-hand hint has its own dwell.
 * Uses the frame timestamps passed in, never a clock.
 */
class GuidanceStabilizer(private val config: GuidanceConfig = GuidanceConfig.Default) {
    private var shown = GuidanceState.NoHand
    private var shownSince = 0L
    private var leaving = false
    private var leftSince = 0L
    private var onlyNoHand = true

    private var hint: HandSide? = null
    private var hintCandidate: HandSide? = null
    private var hintPending = false
    private var hintSince = 0L

    fun update(raw: GuidanceState, wrongHand: HandSide?, timestampMs: Long): StableGuidance {
        // Time how long raw has differed from shown, not how long one candidate lasted, so raw
        // alternating between two other states (TooFar/NoHand flicker) still moves shown off PalmDetected.
        if (raw == shown) {
            leaving = false
        } else {
            if (!leaving) {
                leaving = true
                leftSince = timestampMs
                onlyNoHand = true
            }
            onlyNoHand = onlyNoHand && raw == GuidanceState.NoHand
            val need = if (shown == GuidanceState.PalmDetected && onlyNoHand) {
                maxOf(config.dwellMs, config.dropoutMs)
            } else {
                config.dwellMs
            }
            if (timestampMs - leftSince >= need) {
                shown = raw
                shownSince = timestampMs
                leaving = false
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
