package app.palm

import app.palm.core.data.AppLanguage
import app.palm.core.data.Profile
import app.palm.core.navigation.Home
import app.palm.core.navigation.Language
import app.palm.testing.FakeAppLocale
import app.palm.testing.FakeProfileRepository
import app.palm.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test fun startIsNullUntilTheFirstRead() = runTest(main.dispatcher) {
        val vm = MainViewModel(FakeProfileRepository(), FakeAppLocale())
        assertNull(vm.start.value)
        advanceUntilIdle()
        assertEquals(Language, vm.start.value)
    }

    @Test fun onboardedStartsAtHome() = runTest(main.dispatcher) {
        val vm = MainViewModel(FakeProfileRepository(Profile(onboardingComplete = true)), FakeAppLocale())
        advanceUntilIdle()
        assertEquals(Home, vm.start.value)
    }

    @Test fun failedReadStartsAtLanguage() = runTest(main.dispatcher) {
        val vm = MainViewModel(FakeProfileRepository(Profile(onboardingComplete = true)).apply { failReads = true }, FakeAppLocale())
        advanceUntilIdle()
        assertEquals(Language, vm.start.value)
    }

    @Test fun appLocaleOverwritesTheStoredLanguage() = runTest(main.dispatcher) {
        val repo = FakeProfileRepository(Profile(language = AppLanguage.EN))
        MainViewModel(repo, FakeAppLocale(currentValue = AppLanguage.HI))
        advanceUntilIdle()
        assertEquals(AppLanguage.HI, repo.state.value.language)
    }
}
