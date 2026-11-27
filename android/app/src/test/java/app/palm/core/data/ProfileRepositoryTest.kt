package app.palm.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    // Not pre-created: an empty file could read as corrupt, and the name must end in .preferences_pb (Pitfall 10)
    private val file get() = File(tmp.root, "profile.preferences_pb")

    @Test fun profileSurvivesANewStoreInstance() = runTest {
        val jobA = Job()
        DataStoreProfileRepository(createProfileStore(CoroutineScope(UnconfinedTestDispatcher(testScheduler) + jobA)) { file }).apply {
            saveName("Asha", Gender.FEMALE)
            saveBirth(LocalDate.of(1996, 3, 12), LocalTime.of(6, 30), timeUnknown = false)
            setLanguage(AppLanguage.HI)
            completeOnboarding()
        }
        jobA.cancelAndJoin() // the store closes its file when its scope completes
        val repoB = DataStoreProfileRepository(createProfileStore(backgroundScope) { file })
        assertEquals(
            Profile(AppLanguage.HI, "Asha", Gender.FEMALE, LocalDate.of(1996, 3, 12), LocalTime.of(6, 30), false, true),
            repoB.profile.first(),
        )
    }

    @Test fun absentFileReadsAsEmptyProfile() = runTest {
        assertEquals(Profile(), DataStoreProfileRepository(createProfileStore(backgroundScope) { file }).profile.first())
    }

    @Test fun unknownTimeDropsTheSavedTime() = runTest {
        val repo = DataStoreProfileRepository(createProfileStore(backgroundScope) { file })
        repo.saveBirth(LocalDate.of(1996, 3, 12), LocalTime.of(6, 30), timeUnknown = false)
        repo.saveBirth(LocalDate.of(1996, 3, 12), null, timeUnknown = true)
        val p = repo.profile.first()
        assertNull(p.birthTime)
        assertTrue(p.birthTimeUnknown)
    }

    @Test fun corruptFileReadsAsEmptyProfile() = runTest {
        file.writeBytes(ByteArray(64) { 0xFF.toByte() }) // invalid protobuf varints
        assertEquals(Profile(), DataStoreProfileRepository(createProfileStore(backgroundScope) { file }).profile.first())
    }

    @Test fun unknownGenderReadsAsNull() = runTest {
        val store = createProfileStore(backgroundScope) { file }
        store.edit { it[stringPreferencesKey("gender")] = "ALIEN" }
        assertNull(DataStoreProfileRepository(store).profile.first().gender)
    }

    @Test fun clearReturnsEmptyProfile() = runTest {
        val repo = DataStoreProfileRepository(createProfileStore(backgroundScope) { file })
        repo.saveName("Asha", Gender.FEMALE)
        repo.completeOnboarding()
        repo.clear()
        assertEquals(Profile(), repo.profile.first())
    }
}
