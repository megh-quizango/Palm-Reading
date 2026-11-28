package app.palm.feature.invalidphoto

import app.palm.R
import app.palm.contract.InvalidReason
import org.junit.Assert.assertEquals
import org.junit.Test

/** D-14: one exhaustive reason -> icon/title/tip mapping, icons per UI-SPEC section 3. */
class ReasonCopyTest {
    private val expected = mapOf(
        InvalidReason.BLURRY_OR_DARK to ReasonCopy(R.drawable.ic_flash, R.string.invalid_blurry_or_dark_title, R.string.invalid_blurry_or_dark_tip),
        InvalidReason.TOO_FAR to ReasonCopy(R.drawable.ic_search, R.string.invalid_too_far_title, R.string.invalid_too_far_tip),
        InvalidReason.JEWELRY to ReasonCopy(R.drawable.ic_star, R.string.invalid_jewelry_title, R.string.invalid_jewelry_tip),
        InvalidReason.BENT to ReasonCopy(R.drawable.ic_hand, R.string.invalid_bent_title, R.string.invalid_bent_tip),
        InvalidReason.FINGERS_TIGHT to ReasonCopy(R.drawable.ic_hand, R.string.invalid_fingers_tight_title, R.string.invalid_fingers_tight_tip),
        InvalidReason.INCOMPLETE to ReasonCopy(R.drawable.ic_gallery, R.string.invalid_incomplete_title, R.string.invalid_incomplete_tip),
        InvalidReason.NO_PALM_LINES to ReasonCopy(R.drawable.ic_eye, R.string.invalid_no_palm_lines_title, R.string.invalid_no_palm_lines_tip),
    )

    @Test fun everyReasonHasTheSpecCopy() {
        assertEquals(InvalidReason.entries.toSet(), expected.keys)
        InvalidReason.entries.forEach { assertEquals(it.name, expected.getValue(it), reasonCopy(it)) }
    }

    @Test fun titlesAndTipsAreDistinct() {
        val copies = InvalidReason.entries.map(::reasonCopy)
        assertEquals(7, copies.map { it.title }.toSet().size)
        assertEquals(7, copies.map { it.tip }.toSet().size)
    }
}
