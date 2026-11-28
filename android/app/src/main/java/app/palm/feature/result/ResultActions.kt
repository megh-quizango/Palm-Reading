package app.palm.feature.result

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.palm.R
import app.palm.contract.PalmReadingDto
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle
import app.palm.feature.result.share.ShareCapture
import app.palm.feature.result.share.shareChooser
import app.palm.feature.result.share.shareIntent
import app.palm.feature.result.share.shareUri
import app.palm.feature.result.share.writeSharePng
import java.io.IOException
import kotlinx.coroutines.launch

private enum class ShareStatus { Idle, Preparing, Failed }

/** Result actions, always stacked (D-20): Scan again, Share (one card at a time, D-21, D-22), Ask [Guide]. */
@Composable
fun ResultActions(
    reading: PalmReadingDto,
    photo: ImageBitmap?,
    onScanAgain: () -> Unit,
    onAskGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var share by remember { mutableStateOf(ShareStatus.Idle) }
    val shareText = stringResource(R.string.result_share_text)
    val chooserTitle = stringResource(R.string.result_share_chooser)
    // The 0-size capture is a Box sibling, so the column's 8 dp spacing never counts it
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
            NocturneButton(stringResource(R.string.result_scan_again), onClick = onScanAgain, modifier = Modifier.fillMaxWidth())
            val preparing = share == ShareStatus.Preparing
            NocturneButton(
                stringResource(if (preparing) R.string.result_share_preparing else R.string.result_share),
                onClick = { if (!preparing) share = ShareStatus.Preparing },
                modifier = Modifier.fillMaxWidth(),
                style = NocturneButtonStyle.Secondary,
                enabled = !preparing,
            )
            NocturneButton(
                stringResource(R.string.feature_ask_guide),
                onClick = onAskGuide,
                modifier = Modifier.fillMaxWidth(),
                style = NocturneButtonStyle.Ghost,
            )
            if (share == ShareStatus.Failed) {
                Text(
                    stringResource(R.string.result_share_failed),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = LocalNocturne.current.type.caption,
                    color = NocturneColors.StatusError,
                )
            }
        }
        if (share == ShareStatus.Preparing) {
            ShareCapture(reading, photo?.let { PhotoState.Ready(it) } ?: PhotoState.Unavailable) { image ->
                scope.launch {
                    share = try {
                        val file = writeSharePng(context.cacheDir, image)
                        context.startActivity(shareChooser(shareIntent(shareUri(context, file), shareText), chooserTitle))
                        ShareStatus.Idle
                    } catch (e: IOException) {
                        ShareStatus.Failed
                    } catch (e: IllegalArgumentException) {
                        ShareStatus.Failed
                    } catch (e: ActivityNotFoundException) {
                        ShareStatus.Failed
                    }
                }
            }
        }
    }
}
