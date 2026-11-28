package app.palm.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.palm.contract.HandSide.RIGHT
import app.palm.contract.InvalidReason
import org.junit.Assert.assertEquals
import org.junit.Test

class PushFromTest {
    @Test fun doubleTapPushesOnce() {
        val stack = NavBackStack<NavKey>(Language, Intro)
        stack.pushFrom(Intro, Name)
        stack.pushFrom(Intro, Name)
        assertEquals(listOf(Language, Intro, Name), stack.toList())
    }

    @Test fun lateCallbackAfterPushIsIgnored() {
        val stack = NavBackStack<NavKey>(Language)
        stack.pushFrom(Language, Intro)
        stack.pushFrom(Intro, Name)
        stack.pushFrom(Language, Intro) // second in-flight continueTo finishing after the first
        assertEquals(listOf(Language, Intro, Name), stack.toList())
    }

    @Test fun replaceAllWithSeveralKeys() {
        val stack = NavBackStack<NavKey>(Home, HandSelect, Scan(app.palm.contract.HandSide.RIGHT), ScanReady)
        stack.replaceAll(Home, Result("a"))
        assertEquals(listOf(Home, Result("a")), stack.toList())
    }

    @Test fun replaceAllWithOneKey() {
        val stack = NavBackStack<NavKey>(Splash)
        stack.replaceAll(Home)
        assertEquals(listOf(Home), stack.toList())
    }

    @Test fun popToHomeDropsEverythingAbove() {
        val stack = NavBackStack<NavKey>(Home, HandSelect, Scan(RIGHT), InvalidPhoto(InvalidReason.JEWELRY, RIGHT))
        stack.popTo(Home)
        assertEquals(listOf(Home), stack.toList())
    }

    @Test fun popToIsANoOpWhenOnTopOrAbsent() {
        val top = NavBackStack<NavKey>(Home)
        top.popTo(Home)
        assertEquals(listOf(Home), top.toList())
        val absent = NavBackStack<NavKey>(HandSelect, Scan(RIGHT))
        absent.popTo(Home)
        assertEquals(listOf(HandSelect, Scan(RIGHT)), absent.toList())
    }

    @Test fun showInvalidReplacesScanReadySoBackLandsOnScan() {
        val invalid = InvalidPhoto(InvalidReason.JEWELRY, RIGHT)
        val stack = NavBackStack<NavKey>(Home, HandSelect, Scan(RIGHT), ScanReady)
        stack.showInvalid(InvalidReason.JEWELRY, RIGHT)
        assertEquals(listOf(Home, HandSelect, Scan(RIGHT), invalid), stack.toList())
        stack.showInvalid(InvalidReason.JEWELRY, RIGHT) // late second callback
        assertEquals(listOf(Home, HandSelect, Scan(RIGHT), invalid), stack.toList())
        stack.popFrom(invalid) // Retake, top-bar back and system Back (D-03)
        assertEquals(Scan(RIGHT), stack.last())
    }

    @Test fun showInvalidIgnoresAStackWithoutScanReadyOnTop() {
        val stack = NavBackStack<NavKey>(Home, Result("a"))
        stack.showInvalid(InvalidReason.BENT, RIGHT)
        assertEquals(listOf(Home, Result("a")), stack.toList())
    }

    @Test fun doubleBackPopsOnce() {
        val stack = NavBackStack<NavKey>(Home, HandSelect)
        stack.popFrom(HandSelect)
        stack.popFrom(HandSelect)
        assertEquals(listOf(Home), stack.toList())
    }
}
