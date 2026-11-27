package app.palm.feature.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.devanagariSafe
import app.palm.core.navigation.Feature

@get:StringRes
internal val Feature.titleRes: Int
    get() = when (this) {
        Feature.PALM -> R.string.feature_palm
        Feature.TAROT -> R.string.feature_tarot
        Feature.HOROSCOPE -> R.string.feature_horoscope
        Feature.BIRTH_CHART -> R.string.feature_birth_chart
        Feature.LOVE_TEST -> R.string.feature_love_test
        Feature.DAILY_REPORT -> R.string.feature_daily_report
        Feature.ASK_GUIDE -> R.string.feature_ask_guide
    }

@get:DrawableRes
internal val Feature.artRes: Int
    get() = when (this) {
        Feature.PALM -> R.drawable.illus_constellation_hand
        Feature.TAROT -> R.drawable.illus_tarot_pair
        Feature.HOROSCOPE -> R.drawable.illus_horoscope_star
        Feature.BIRTH_CHART -> R.drawable.illus_zodiac_wheel
        Feature.LOVE_TEST -> R.drawable.illus_heart_orbit
        Feature.DAILY_REPORT -> R.drawable.illus_sun_medallion
        Feature.ASK_GUIDE -> R.drawable.illus_guide_orb
    }

/** Content of the ComingSoon(feature) bottom sheet (D-12); the sheet itself is the scene strategy's. */
@Composable
fun ComingSoonSheet(feature: Feature, onOk: () -> Unit) {
    val type = LocalNocturne.current.type
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = NocturneSpace.S24, end = NocturneSpace.S24, top = NocturneSpace.S8, bottom = NocturneSpace.S24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
    ) {
        Image(painterResource(feature.artRes), null, Modifier.size(96.dp))
        Text(
            stringResource(R.string.coming_soon_overline),
            style = type.overline.devanagariSafe(),
            color = NocturneSemantic.MutedForeground,
        )
        Text(
            stringResource(feature.titleRes),
            style = type.displayM,
            color = NocturneSemantic.Foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.coming_soon_body),
            style = type.bodyL,
            color = NocturneSemantic.MutedForeground,
            textAlign = TextAlign.Center,
        )
        NocturneButton(stringResource(R.string.common_ok), onClick = onOk, modifier = Modifier.fillMaxWidth())
    }
}
