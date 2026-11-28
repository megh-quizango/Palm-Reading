package app.palm.debug

import app.palm.core.reading.FixtureVariant

/** Release twin of the debug file: no lab, so every read uses the default reading (D-06, T-05-16). */
fun debugFixtureVariant(): FixtureVariant? = null
