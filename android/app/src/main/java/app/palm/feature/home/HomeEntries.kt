package app.palm.feature.home

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.Home

@Suppress("UNUSED_PARAMETER") // 03-04 navigates from Home to HandSelect and ComingSoon
fun EntryProviderScope<NavKey>.homeEntries(backStack: NavBackStack<NavKey>) {
    entry<Home> { HomeScreen(viewModel = hiltViewModel()) }
}
