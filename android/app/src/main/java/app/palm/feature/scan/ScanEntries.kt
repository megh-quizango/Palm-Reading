package app.palm.feature.scan

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.Home
import app.palm.core.navigation.Result
import app.palm.core.navigation.Scan
import app.palm.core.navigation.ScanReady
import app.palm.core.navigation.popFrom
import app.palm.core.navigation.pushFrom
import app.palm.core.navigation.replaceAll

fun EntryProviderScope<NavKey>.scanEntries(backStack: NavBackStack<NavKey>) {
    entry<Scan> { key ->
        ScanScreen(
            hand = key.hand,
            onBack = { backStack.popFrom(key) },
            onReady = { backStack.pushFrom(key, ScanReady) },
        )
    }
    entry<ScanReady> {
        ScanReadyScreen(
            onRetake = { backStack.popFrom(ScanReady) },
            onSaved = { id -> if (backStack.lastOrNull() == ScanReady) backStack.replaceAll(Home, Result(id)) },
        )
    }
}
