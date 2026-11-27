package app.palm.feature.home

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.HandSelect
import app.palm.core.navigation.Home
import app.palm.core.navigation.pushFrom

fun EntryProviderScope<NavKey>.homeEntries(backStack: NavBackStack<NavKey>) {
    entry<Home> {
        HomeScreen(
            viewModel = hiltViewModel(),
            onScan = { backStack.pushFrom(Home, HandSelect) },
            onFeature = { },
            onGreetingLongPress = null,
        )
    }
}
