package app.palm.feature.result

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.ComingSoon
import app.palm.core.navigation.Feature
import app.palm.core.navigation.HandSelect
import app.palm.core.navigation.Result
import app.palm.core.navigation.popFrom
import app.palm.core.navigation.pushFrom

fun EntryProviderScope<NavKey>.resultEntries(backStack: NavBackStack<NavKey>) {
    entry<Result> { key ->
        ResultScreen(
            readingId = key.readingId,
            onBack = { backStack.popFrom(key) },
            onScanAgain = { backStack.pushFrom(key, HandSelect) },
            onAskGuide = { backStack.pushFrom(key, ComingSoon(Feature.ASK_GUIDE)) },
        )
    }
}
