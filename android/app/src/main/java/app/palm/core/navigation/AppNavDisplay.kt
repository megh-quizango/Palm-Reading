package app.palm.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBackground
import app.palm.core.designsystem.NocturneDurations
import app.palm.core.designsystem.NocturneEasings
import app.palm.feature.handselect.handSelectEntries
import app.palm.feature.home.homeEntries
import app.palm.feature.onboarding.onboardingEntries

/** Enter: fade in + 24 dp slide from the end (start on pop). Exit: fade out (UI-SPEC Motion). */
private fun transition(slidePx: Int): ContentTransform =
    fadeIn(tween(NocturneDurations.Standard, easing = NocturneEasings.EmphasizedDecelerate)) +
        slideInHorizontally(tween(NocturneDurations.Standard, easing = NocturneEasings.EmphasizedDecelerate)) { slidePx } togetherWith
        fadeOut(tween(NocturneDurations.Quick, easing = NocturneEasings.EmphasizedAccelerate))

/** [start] is null while the splash holds; then [Splash] is replaced by the start route. */
@Composable
fun AppNavDisplay(start: Route?) {
    val backStack = rememberNavBackStack(Splash)
    LaunchedEffect(start) {
        // Only from Splash: a stack restored after process death is kept
        if (start != null && backStack.lastOrNull() == Splash) backStack.replaceAll(start)
    }
    val px = with(LocalDensity.current) { 24.dp.roundToPx() }
    val endPx = if (LocalLayoutDirection.current == LayoutDirection.Ltr) px else -px
    val still = LocalNocturne.current.reduceMotion
    fun spec(slidePx: Int) = if (still) EnterTransition.None togetherWith ExitTransition.None else transition(slidePx)
    // One sky for every screen, so the stars never restart on a transition; entries stay transparent.
    // An opt-out route on top (Scan) pauses it; the call site never changes, so the nav subtree is kept (D-14)
    NocturneBackground(animate = (backStack.lastOrNull() as? Route)?.showsBackground ?: true) {
        NavDisplay(
            backStack = backStack,
            // Back on a one-entry stack (Language, the first onboarding step) leaves the app (D-01)
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { spec(endPx) },
            popTransitionSpec = { spec(-endPx) },
            predictivePopTransitionSpec = { spec(-endPx) },
            entryProvider = entryProvider {
                entry<Splash> { } // the kept system splash covers it; the sky shows through
                onboardingEntries(backStack)
                homeEntries(backStack)
                handSelectEntries(backStack)
            },
        )
    }
}
