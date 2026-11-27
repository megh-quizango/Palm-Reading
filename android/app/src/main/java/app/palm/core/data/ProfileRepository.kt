package app.palm.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class AppLanguage(val tag: String) { EN("en"), HI("hi") }

enum class Gender { MALE, FEMALE, OTHER }

data class Profile(
    val language: AppLanguage? = null,
    val firstName: String = "",
    val gender: Gender? = null,
    val birthDate: LocalDate? = null,
    val birthTime: LocalTime? = null,
    val birthTimeUnknown: Boolean = false,
    val onboardingComplete: Boolean = false,
)

/** The on-device profile (D-04). Never leaves the phone: excluded from backup and device transfer. */
interface ProfileRepository {
    val profile: Flow<Profile>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun saveName(firstName: String, gender: Gender?)
    suspend fun saveBirth(date: LocalDate, time: LocalTime?, timeUnknown: Boolean)
    suspend fun completeOnboarding()
    suspend fun clear()
}

// On-disk format: renaming the file or a key needs a DataStore migration once users install
private object Keys {
    val Language = stringPreferencesKey("language")
    val FirstName = stringPreferencesKey("first_name")
    val Gender = stringPreferencesKey("gender")
    val BirthDate = stringPreferencesKey("birth_date")
    val BirthTime = stringPreferencesKey("birth_time")
    val BirthTimeUnknown = booleanPreferencesKey("birth_time_unknown")
    val OnboardingComplete = booleanPreferencesKey("onboarding_complete")
}

private inline fun <T> parseOrNull(value: String?, parse: (String) -> T): T? =
    value?.let { runCatching { parse(it) }.getOrNull() }

/** Tolerant read: an unknown enum name or an unparsable date/time becomes null. */
private fun Preferences.toProfile() = Profile(
    language = AppLanguage.entries.firstOrNull { it.tag == this[Keys.Language] },
    firstName = this[Keys.FirstName].orEmpty(),
    gender = parseOrNull(this[Keys.Gender]) { Gender.valueOf(it) },
    birthDate = parseOrNull(this[Keys.BirthDate]) { LocalDate.parse(it) },
    birthTime = parseOrNull(this[Keys.BirthTime]) { LocalTime.parse(it) },
    birthTimeUnknown = this[Keys.BirthTimeUnknown] ?: false,
    onboardingComplete = this[Keys.OnboardingComplete] ?: false,
)

class DataStoreProfileRepository @Inject constructor(private val store: DataStore<Preferences>) : ProfileRepository {
    override val profile: Flow<Profile> = store.data
        // An unreadable file reads as an empty profile (routes to Language, never a crash)
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toProfile() }

    override suspend fun setLanguage(language: AppLanguage) {
        store.edit { it[Keys.Language] = language.tag }
    }

    override suspend fun saveName(firstName: String, gender: Gender?) {
        store.edit {
            it[Keys.FirstName] = firstName
            if (gender == null) it.remove(Keys.Gender) else it[Keys.Gender] = gender.name
        }
    }

    override suspend fun saveBirth(date: LocalDate, time: LocalTime?, timeUnknown: Boolean) {
        store.edit {
            it[Keys.BirthDate] = date.toString()
            val t = time?.takeUnless { timeUnknown }
            if (t == null) it.remove(Keys.BirthTime) else it[Keys.BirthTime] = t.toString()
            it[Keys.BirthTimeUnknown] = timeUnknown
        }
    }

    override suspend fun completeOnboarding() {
        store.edit { it[Keys.OnboardingComplete] = true }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }
}

/** The one construction path for the profile store (Hilt module and JVM tests). */
fun createProfileStore(scope: CoroutineScope, produceFile: () -> File): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = produceFile,
    )

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    /** Exactly one DataStore per file per process. */
    @Provides
    @Singleton
    fun store(@ApplicationContext context: Context): DataStore<Preferences> =
        createProfileStore(CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            context.preferencesDataStoreFile("profile")
        }

    @Provides
    @Singleton
    fun repository(store: DataStore<Preferences>): ProfileRepository = DataStoreProfileRepository(store)
}

/** Instrumented tests reset and seed the profile through the real graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProfileEntryPoint {
    fun profileRepository(): ProfileRepository
}
