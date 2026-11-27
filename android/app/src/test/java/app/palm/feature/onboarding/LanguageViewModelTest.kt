package app.palm.feature.onboarding

import app.palm.core.data.AppLanguage
import app.palm.core.locale.initialLanguage
import app.palm.testing.FakeAppLocale
import app.palm.testing.FakeProfileRepository
import app.palm.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LanguageViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun appLocaleWins() = assertEquals(AppLanguage.HI, initialLanguage(AppLanguage.HI, AppLanguage.EN, "en"))

    @Test fun savedBeatsSystem() = assertEquals(AppLanguage.EN, initialLanguage(null, AppLanguage.EN, "hi"))

    @Test fun hindiSystemDefaultsToHindi() = assertEquals(AppLanguage.HI, initialLanguage(null, null, "hi"))

    @Test fun otherSystemDefaultsToEnglish() = assertEquals(AppLanguage.EN, initialLanguage(null, null, "ta"))

    @Test fun selectAppliesAndStores() {
        val repo = FakeProfileRepository()
        val locale = FakeAppLocale()
        val vm = LanguageViewModel(repo, locale)
        vm.select(AppLanguage.HI)
        assertEquals(listOf(AppLanguage.HI), locale.applied)
        assertEquals(AppLanguage.HI, repo.state.value.language)
        assertEquals(AppLanguage.HI, vm.selected.value)
    }

    @Test fun failedSaveStaysAndFlags() {
        val repo = FakeProfileRepository().apply { failWrites = true }
        val vm = LanguageViewModel(repo, FakeAppLocale())
        var calls = 0
        vm.continueTo { calls++ }
        assertTrue(vm.saveFailed.value)
        assertEquals(0, calls)
    }

    @Test fun continueStoresLanguageWithoutCompletingOnboarding() {
        val repo = FakeProfileRepository()
        val vm = LanguageViewModel(repo, FakeAppLocale(currentValue = AppLanguage.HI))
        var calls = 0
        vm.continueTo { calls++ }
        assertEquals(AppLanguage.HI, repo.state.value.language)
        assertFalse(repo.state.value.onboardingComplete)
        assertFalse(vm.saveFailed.value)
        assertEquals(1, calls)
    }
}
