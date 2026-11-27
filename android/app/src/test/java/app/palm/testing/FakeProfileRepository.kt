package app.palm.testing

import app.palm.core.data.AppLanguage
import app.palm.core.data.Gender
import app.palm.core.data.Profile
import app.palm.core.data.ProfileRepository
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** In-memory profile with switchable read and write failures. */
class FakeProfileRepository(initial: Profile = Profile()) : ProfileRepository {
    val state = MutableStateFlow(initial)
    var failWrites = false
    var failReads = false

    override val profile: Flow<Profile> = flow {
        if (failReads) throw IOException("fake")
        emitAll(state)
    }

    private fun write(change: (Profile) -> Profile) {
        if (failWrites) throw IOException("fake")
        state.update(change)
    }

    override suspend fun setLanguage(language: AppLanguage) = write { it.copy(language = language) }

    override suspend fun saveName(firstName: String, gender: Gender?) = write { it.copy(firstName = firstName, gender = gender) }

    // Same rule as the real repository: "I don't know" drops the time
    override suspend fun saveBirth(date: LocalDate, time: LocalTime?, timeUnknown: Boolean) =
        write { it.copy(birthDate = date, birthTime = time.takeUnless { timeUnknown }, birthTimeUnknown = timeUnknown) }

    override suspend fun completeOnboarding() = write { it.copy(onboardingComplete = true) }

    override suspend fun clear() = write { Profile() }
}
