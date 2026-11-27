package app.palm.core.navigation

import app.palm.core.data.Profile
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StartRouteTest {
    @Test fun onboardedGoesHome() = assertEquals(Home, startRoute(Profile(onboardingComplete = true)))

    @Test fun emptyGoesToLanguage() = assertEquals(Language, startRoute(Profile()))

    @Test fun partialProfileStillGoesToLanguage() =
        assertEquals(Language, startRoute(Profile(firstName = "Asha", birthDate = LocalDate.of(1996, 3, 12))))
}
