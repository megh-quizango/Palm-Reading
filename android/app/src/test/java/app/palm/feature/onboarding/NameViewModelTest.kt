package app.palm.feature.onboarding

import app.palm.core.data.Gender
import app.palm.core.data.Profile
import app.palm.testing.FakeProfileRepository
import app.palm.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NameViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun prefillsFromSavedProfile() {
        val vm = NameViewModel(FakeProfileRepository(Profile(firstName = "Asha", gender = Gender.FEMALE)))
        assertEquals("Asha", vm.state.value.name)
        assertEquals(Gender.FEMALE, vm.state.value.gender)
        assertEquals(NameCheck.OK, vm.state.value.check)
    }

    @Test fun validNameHasNoError() {
        val vm = NameViewModel(FakeProfileRepository())
        vm.onNameChange("Asha")
        assertEquals(NameCheck.OK, vm.state.value.check)
        assertFalse(vm.state.value.showError)
    }

    @Test fun errorWaitsForRevealAndClearsWhenValid() {
        val vm = NameViewModel(FakeProfileRepository())
        vm.onNameChange("A1")
        assertFalse(vm.state.value.showError)
        vm.revealError()
        assertTrue(vm.state.value.showError)
        assertEquals(NameCheck.BAD_CHARS, vm.state.value.check)
        vm.onNameChange("Al")
        assertFalse(vm.state.value.showError)
    }

    @Test fun emptyNeverErrors() {
        val vm = NameViewModel(FakeProfileRepository())
        vm.onNameChange("")
        vm.revealError()
        assertFalse(vm.state.value.showError)
    }

    @Test fun tappingSelectedGenderClearsIt() {
        val vm = NameViewModel(FakeProfileRepository())
        vm.onGenderTap(Gender.FEMALE)
        vm.onGenderTap(Gender.FEMALE)
        assertNull(vm.state.value.gender)
    }

    @Test fun inputOverThirtyIsIgnored() {
        val vm = NameViewModel(FakeProfileRepository())
        vm.onNameChange("a".repeat(NAME_MAX))
        vm.onNameChange("a".repeat(NAME_MAX + 1))
        assertEquals(NAME_MAX, vm.state.value.name.length)
    }

    @Test fun failedSaveStaysThenRetrySucceeds() {
        val repo = FakeProfileRepository().apply { failWrites = true }
        val vm = NameViewModel(repo)
        vm.onNameChange("Asha")
        var calls = 0
        vm.save { calls++ }
        assertTrue(vm.state.value.saveFailed)
        assertEquals(0, calls)
        repo.failWrites = false
        vm.save { calls++ }
        assertEquals(1, calls)
        assertFalse(vm.state.value.saveFailed)
        assertEquals("Asha", repo.state.value.firstName)
    }
}
