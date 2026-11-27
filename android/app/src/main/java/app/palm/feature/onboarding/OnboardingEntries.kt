package app.palm.feature.onboarding

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.core.navigation.Birth
import app.palm.core.navigation.Home
import app.palm.core.navigation.Intro
import app.palm.core.navigation.Language
import app.palm.core.navigation.Name
import app.palm.core.navigation.replaceAll

/** Language -> Intro -> Name -> Birth push; finishing Birth replaces the stack with [Home] (D-01). */
fun EntryProviderScope<NavKey>.onboardingEntries(backStack: NavBackStack<NavKey>) {
    entry<Language> {
        val vm: LanguageViewModel = hiltViewModel()
        LanguageScreen(vm, onContinue = { vm.continueTo { backStack.add(Intro) } })
    }
    entry<Intro> {
        IntroScreen(onFinish = { backStack.add(Name) })
    }
    entry<Name> {
        val vm: NameViewModel = hiltViewModel()
        NameScreen(vm, onContinue = { vm.save { backStack.add(Birth) } })
    }
    entry<Birth> {
        val vm: BirthViewModel = hiltViewModel()
        BirthScreen(vm, onContinue = { vm.finish { backStack.replaceAll(Home) } })
    }
}
