package app.palm.testing

import app.palm.core.data.AppLanguage
import app.palm.core.locale.AppLocale

/** Records every applied language; [currentValue] stands in for AppCompat's stored locale. */
class FakeAppLocale(var currentValue: AppLanguage? = null, var system: String = "en") : AppLocale {
    val applied = mutableListOf<AppLanguage>()

    override fun current(): AppLanguage? = currentValue

    override fun apply(language: AppLanguage) {
        applied += language
        currentValue = language
    }

    override fun systemLanguage(): String = system
}
