package app.palm.feature.onboarding

import app.palm.core.data.Profile
import app.palm.testing.FakeProfileRepository
import app.palm.testing.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BirthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val date = LocalDate.of(1996, 3, 12)
    private val time = LocalTime.of(6, 30)

    @Test fun prefillsFromSavedProfile() {
        val vm = BirthViewModel(FakeProfileRepository(Profile(birthDate = date, birthTime = time)))
        assertEquals(date, vm.state.value.date)
        assertEquals(time, vm.state.value.time)
        assertFalse(vm.state.value.timeUnknown)
        val unknown = BirthViewModel(FakeProfileRepository(Profile(birthDate = date, birthTimeUnknown = true)))
        assertTrue(unknown.state.value.timeUnknown)
    }

    @Test fun timeUnknownClearsTimeAndUncheckingLeavesItEmpty() {
        val vm = BirthViewModel(FakeProfileRepository())
        vm.onTime(time)
        vm.onTimeUnknown(true)
        assertTrue(vm.state.value.timeUnknown)
        assertNull(vm.state.value.time)
        vm.onTimeUnknown(false)
        assertFalse(vm.state.value.timeUnknown)
        assertNull(vm.state.value.time)
    }

    @Test fun failedSaveNeverCompletesOnboarding() {
        val repo = FakeProfileRepository().apply { failWrites = true }
        val vm = BirthViewModel(repo)
        vm.onDate(date)
        var calls = 0
        vm.finish { calls++ }
        assertTrue(vm.state.value.saveFailed)
        assertFalse(repo.state.value.onboardingComplete)
        assertEquals(0, calls)
    }

    @Test fun finishWithoutDateDoesNothing() {
        val repo = FakeProfileRepository()
        val vm = BirthViewModel(repo)
        var calls = 0
        vm.finish { calls++ }
        assertEquals(0, calls)
        assertFalse(repo.state.value.onboardingComplete)
    }

    @Test fun finishStoresBirthAndCompletesOnce() {
        val repo = FakeProfileRepository()
        val vm = BirthViewModel(repo)
        vm.onDate(date)
        vm.onTime(time)
        var calls = 0
        vm.finish { calls++ }
        val p = repo.state.value
        assertEquals(date, p.birthDate)
        assertEquals(time, p.birthTime)
        assertFalse(p.birthTimeUnknown)
        assertTrue(p.onboardingComplete)
        assertEquals(1, calls)
    }
}
