package app.palm.debug

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.palm.showcase.ShowcaseScreen
import kotlinx.serialization.Serializable

/** Phase 1 design-system showcase, debug builds only (D-03). */
@Serializable data object Showcase : NavKey

/** Long-press target on the Home greeting; null in release. */
val debugHomeLongPress: NavKey? = Showcase

fun EntryProviderScope<NavKey>.debugEntries() {
    entry<Showcase> { ShowcaseScreen(statusViewModel = hiltViewModel()) }
}
