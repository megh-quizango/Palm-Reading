package app.palm.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.palm.contract.HandSide
import app.palm.feature.scan.ScanDebugSample
import kotlinx.coroutines.flow.StateFlow

/** Release twin of the debug HUD: renders nothing and ships no landmark recorder (T-04-20). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ScanDebugHud(samples: StateFlow<ScanDebugSample?>, hand: HandSide, modifier: Modifier = Modifier) = Unit
