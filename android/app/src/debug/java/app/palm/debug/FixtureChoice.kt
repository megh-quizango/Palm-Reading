package app.palm.debug

import app.palm.core.reading.FixtureVariant

/** The lab's variant choice, in memory only; it applies to the next read, lab or real scan (D-05, D-06). */
object FixtureLabChoice {
    @Volatile var variant: FixtureVariant? = null
}

fun debugFixtureVariant(): FixtureVariant? = FixtureLabChoice.variant
