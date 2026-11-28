package app.palm.debug

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.ScanReady
import app.palm.core.navigation.popFrom
import app.palm.core.navigation.pushFrom
import app.palm.showcase.ShowcaseScreen
import kotlinx.serialization.Serializable

/** Phase 1 design-system showcase, debug builds only (D-03). */
@Serializable data object Showcase : NavKey

/** Long-press target on the Home greeting; null in release. */
val debugHomeLongPress: NavKey? = Showcase

fun EntryProviderScope<NavKey>.debugEntries(backStack: NavBackStack<NavKey>) {
    entry<Showcase> {
        ShowcaseScreen(statusViewModel = hiltViewModel(), onFixtureLab = { backStack.pushFrom(Showcase, FixtureLab) })
    }
    entry<FixtureLab> {
        FixtureLabScreen(
            onBack = { backStack.popFrom(FixtureLab) },
            onOpenScanReady = { backStack.pushFrom(FixtureLab, ScanReady) },
        )
    }
}
