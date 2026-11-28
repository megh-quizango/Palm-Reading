package app.palm.feature.home

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.BottomSheetSceneStrategy
import app.palm.core.navigation.ComingSoon
import app.palm.core.navigation.HandSelect
import app.palm.core.navigation.Home
import app.palm.core.navigation.Result
import app.palm.core.navigation.popFrom
import app.palm.core.navigation.pushFrom
import app.palm.debug.debugHomeLongPress

fun EntryProviderScope<NavKey>.homeEntries(backStack: NavBackStack<NavKey>) {
    entry<Home> {
        HomeScreen(
            viewModel = hiltViewModel(),
            onScan = { backStack.pushFrom(Home, HandSelect) },
            onFeature = { backStack.pushFrom(Home, ComingSoon(it)) },
            onGreetingLongPress = debugHomeLongPress?.let { key -> { backStack.pushFrom(Home, key) } },
            onOpenReading = { id -> backStack.pushFrom(Home, Result(id)) },
        )
    }
    entry<ComingSoon>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        ComingSoonSheet(key.feature, onOk = { backStack.popFrom(key) })
    }
}
