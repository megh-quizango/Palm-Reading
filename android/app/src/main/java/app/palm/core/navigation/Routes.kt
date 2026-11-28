package app.palm.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.contract.HandSide
import app.palm.core.data.Profile
import kotlinx.serialization.Serializable

/** Every Phase 3 destination (D-01). [showsBackground] false hides the root sky (D-14). */
@Serializable
sealed interface Route : NavKey {
    val showsBackground: Boolean get() = true
}

@Serializable data object Splash : Route
@Serializable data object Language : Route
@Serializable data object Intro : Route
@Serializable data object Name : Route
@Serializable data object Birth : Route
@Serializable data object Home : Route
@Serializable data object HandSelect : Route

@Serializable
data class Scan(val hand: HandSide) : Route {
    override val showsBackground: Boolean get() = false
}

/** The processed photo before upload (D-01); the capture itself lives in ScanResultHolder. */
@Serializable data object ScanReady : Route

@Serializable
enum class Feature { PALM, TAROT, HOROSCOPE, BIRTH_CHART, LOVE_TEST, DAILY_REPORT, ASK_GUIDE }

@Serializable
data class ComingSoon(val feature: Feature) : Route

/** Replace the whole stack with [key]; adds first so the list is never empty. */
fun NavBackStack<NavKey>.replaceAll(key: NavKey) {
    add(key)
    subList(0, size - 1).clear()
}

/** Push [to] only while [from] is on top, so a double tap cannot push the same step twice. */
fun NavBackStack<NavKey>.pushFrom(from: NavKey, to: NavKey) {
    if (lastOrNull() == from) add(to)
}

/** Pop only while [from] is on top, so a double tap on Back or OK cannot pop two screens. */
fun NavBackStack<NavKey>.popFrom(from: NavKey) {
    if (lastOrNull() == from) removeAt(size - 1)
}

fun startRoute(profile: Profile): Route = if (profile.onboardingComplete) Home else Language
