package app.palm.feature.onboarding

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.Home
import app.palm.core.navigation.Language
import app.palm.core.navigation.replaceAll

fun EntryProviderScope<NavKey>.onboardingEntries(backStack: NavBackStack<NavKey>) {
    entry<Language> {
        val vm: LanguageViewModel = hiltViewModel()
        // Language is the last onboarding step for now; 03-03 inserts Intro, Name and Birth
        LanguageScreen(vm, onContinue = { vm.continueTo(completeOnboarding = true) { backStack.replaceAll(Home) } })
    }
}
