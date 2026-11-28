package app.palm.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.contract.HandSide
import app.palm.contract.InvalidReason
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

/** S10 Result for a saved reading (D-07); the reading itself lives in Room. */
@Serializable data class Result(val readingId: String) : Route

/** Unusable photo (D-14); nothing is saved, so the route carries all the screen needs. */
@Serializable data class InvalidPhoto(val reason: InvalidReason, val hand: HandSide) : Route

@Serializable
enum class Feature { PALM, TAROT, HOROSCOPE, BIRTH_CHART, LOVE_TEST, DAILY_REPORT, ASK_GUIDE }

@Serializable
data class ComingSoon(val feature: Feature) : Route

/** Replace the whole stack with [keys]; adds first so the list is never empty. */
fun NavBackStack<NavKey>.replaceAll(vararg keys: NavKey) {
    require(keys.isNotEmpty())
    addAll(keys)
    subList(0, size - keys.size).clear()
}

/** Push [to] only while [from] is on top, so a double tap cannot push the same step twice. */
fun NavBackStack<NavKey>.pushFrom(from: NavKey, to: NavKey) {
    if (lastOrNull() == from) add(to)
}

/** Pop only while [from] is on top, so a double tap on Back or OK cannot pop two screens. */
fun NavBackStack<NavKey>.popFrom(from: NavKey) {
    if (lastOrNull() == from) removeAt(size - 1)
}

/** D-03: ScanReady becomes InvalidPhoto, so Back lands on Scan(hand); adds first so the stack is never empty. */
fun NavBackStack<NavKey>.showInvalid(reason: InvalidReason, hand: HandSide) {
    if (lastOrNull() != ScanReady) return
    add(InvalidPhoto(reason, hand))
    removeAt(size - 2)
}

/** Removes everything above the last [key]; no-op when [key] is absent or already on top. */
fun NavBackStack<NavKey>.popTo(key: NavKey) {
    val i = lastIndexOf(key)
    if (i >= 0) subList(i + 1, size).clear()
}

fun startRoute(profile: Profile): Route = if (profile.onboardingComplete) Home else Language
