package app.palm.feature.invalidphoto

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.palm.R
import app.palm.contract.InvalidReason
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneStateBlock
import app.palm.core.designsystem.components.NocturneTopBar

data class ReasonCopy(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val tip: Int)

/** UI-SPEC section 3 table (D-14). */
fun reasonCopy(reason: InvalidReason): ReasonCopy = when (reason) {
    InvalidReason.BLURRY_OR_DARK -> ReasonCopy(R.drawable.ic_flash, R.string.invalid_blurry_or_dark_title, R.string.invalid_blurry_or_dark_tip)
    InvalidReason.TOO_FAR -> ReasonCopy(R.drawable.ic_search, R.string.invalid_too_far_title, R.string.invalid_too_far_tip)
    InvalidReason.JEWELRY -> ReasonCopy(R.drawable.ic_star, R.string.invalid_jewelry_title, R.string.invalid_jewelry_tip)
    InvalidReason.BENT -> ReasonCopy(R.drawable.ic_hand, R.string.invalid_bent_title, R.string.invalid_bent_tip)
    InvalidReason.FINGERS_TIGHT -> ReasonCopy(R.drawable.ic_hand, R.string.invalid_fingers_tight_title, R.string.invalid_fingers_tight_tip)
    InvalidReason.INCOMPLETE -> ReasonCopy(R.drawable.ic_gallery, R.string.invalid_incomplete_title, R.string.invalid_incomplete_tip)
    InvalidReason.NO_PALM_LINES -> ReasonCopy(R.drawable.ic_eye, R.string.invalid_no_palm_lines_title, R.string.invalid_no_palm_lines_tip)
}

/** Unusable photo (D-13, D-14): no reading, so no disclaimer. NavDisplay's own Back pop lands on Scan(hand), like Retake. */
@Composable
fun InvalidPhotoScreen(reason: InvalidReason, onRetake: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val copy = reasonCopy(reason)
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, bottom = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.invalid_title), onBack = onRetake)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24),
            verticalArrangement = Arrangement.Center,
        ) {
            NocturneStateBlock(
                painterResource(copy.icon),
                NocturneColors.StatusError,
                stringResource(copy.title),
                stringResource(copy.tip),
                primary = stringResource(R.string.scan_retake) to onRetake,
                secondary = stringResource(R.string.common_back_home) to onHome,
            )
        }
    }
}
