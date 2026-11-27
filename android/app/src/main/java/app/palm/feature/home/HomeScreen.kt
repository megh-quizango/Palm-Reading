package app.palm.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.devanagariSafe
import app.palm.core.navigation.Feature
import java.time.LocalTime

/** S06 Home: header, hero, feature grid; Back asks before leaving (D-11..D-13). */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onScan: () -> Unit,
    onFeature: (Feature) -> Unit,
    onGreetingLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val p = profile ?: return
    val type = LocalNocturne.current.type
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, top = NocturneSpace.S8, bottom = NocturneSpace.S24),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalArrangement = Arrangement.Center) {
            Text(
                stringResource(greetingRes(dayPart(LocalTime.now())), p.firstName),
                style = type.overline.devanagariSafe(),
                color = NocturneSemantic.MutedForeground,
            )
            Text(
                stringResource(R.string.app_name),
                style = type.displayL,
                color = NocturneSemantic.Foreground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(NocturneSpace.S16))
        HeroCard(onScan)
    }
}
