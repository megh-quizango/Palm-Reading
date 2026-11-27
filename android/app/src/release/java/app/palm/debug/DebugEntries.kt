package app.palm.debug

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** Release twin of the debug file: no showcase, no Home long-press target (D-03). */
val debugHomeLongPress: NavKey? = null

@Suppress("UnusedReceiverParameter")
fun EntryProviderScope<NavKey>.debugEntries() = Unit
