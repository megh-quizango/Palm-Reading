package app.palm.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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

    @Test fun doubleBackPopsOnce() {
        val stack = NavBackStack<NavKey>(Home, HandSelect)
        stack.popFrom(HandSelect)
        stack.popFrom(HandSelect)
        assertEquals(listOf(Home), stack.toList())
    }
}
