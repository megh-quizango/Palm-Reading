package app.palm.feature.handselect

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.HandSelect
import app.palm.core.navigation.Scan
import app.palm.core.navigation.popFrom
import app.palm.core.navigation.pushFrom

fun EntryProviderScope<NavKey>.handSelectEntries(backStack: NavBackStack<NavKey>) {
    entry<HandSelect> {
        HandSelectScreen(
            onBack = { backStack.popFrom(HandSelect) },
            onContinue = { hand -> backStack.pushFrom(HandSelect, Scan(hand)) },
        )
    }
}
