package app.palm.core.locale

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.palm.core.data.AppLanguage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** The per-app language (D-06). AppCompat is the source of truth; it also reflects Android 13+ Settings changes. */
interface AppLocale {
    fun current(): AppLanguage?
    /** Main thread only: it may recreate the activity. */
    fun apply(language: AppLanguage)
    /** The system language, unaffected by the app locale. */
    fun systemLanguage(): String
}

class AppCompatLocale @Inject constructor() : AppLocale {
    override fun current(): AppLanguage? {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: return null
        return AppLanguage.entries.firstOrNull { it.tag == tag }
    }

    override fun apply(language: AppLanguage) =
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))

    override fun systemLanguage(): String = Resources.getSystem().configuration.locales[0].language
}

/** The app locale if set, else the saved language, else Hindi for a Hindi system, else English. */
fun initialLanguage(appLocale: AppLanguage?, saved: AppLanguage?, systemLanguage: String): AppLanguage =
    appLocale ?: saved ?: if (systemLanguage == AppLanguage.HI.tag) AppLanguage.HI else AppLanguage.EN

@Module
@InstallIn(SingletonComponent::class)
abstract class LocaleModule {
    @Binds
    abstract fun appLocale(impl: AppCompatLocale): AppLocale
}
