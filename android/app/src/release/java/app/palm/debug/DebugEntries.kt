package app.palm.debug

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Release twin of the debug file: no showcase, no fixture lab, no Home long-press target (D-03, D-05). */
val debugHomeLongPress: NavKey? = null

@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.debugEntries(backStack: NavBackStack<NavKey>) = Unit
