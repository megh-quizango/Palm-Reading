package app.palm.feature.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import app.palm.R
import app.palm.contract.PalmReadingDto
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle

/** Result actions, always stacked (D-20). [reading] and [photo] are for Share (05-04). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ResultActions(
    reading: PalmReadingDto,
    photo: ImageBitmap?,
    onScanAgain: () -> Unit,
    onAskGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
        NocturneButton(stringResource(R.string.result_scan_again), onClick = onScanAgain, modifier = Modifier.fillMaxWidth())
        NocturneButton(
            stringResource(R.string.feature_ask_guide),
            onClick = onAskGuide,
            modifier = Modifier.fillMaxWidth(),
            style = NocturneButtonStyle.Ghost,
        )
    }
}
