package app.palm.feature.handselect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.palm.R
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneTopBar

/** Scan(hand) until Phase 4: opaque bg.camera, the root sky is paused under it (D-14, D-15). */
@Composable
fun ScanPlaceholderScreen(hand: HandSide, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val type = LocalNocturne.current.type
    Column(
        modifier
            .fillMaxSize()
            .background(NocturneColors.BgCamera)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = NocturneSpace.S16),
    ) {
        NocturneTopBar(stringResource(R.string.hand_title), onBack)
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
                Text(
                    stringResource(R.string.scan_placeholder_title),
                    style = type.titleM,
                    color = NocturneSemantic.Foreground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.scan_placeholder_hand, stringResource(handLabel(hand))),
                    style = type.bodyL,
                    color = NocturneSemantic.MutedForeground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
