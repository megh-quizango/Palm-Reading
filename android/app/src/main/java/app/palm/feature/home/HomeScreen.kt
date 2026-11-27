package app.palm.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import java.time.LocalTime

/** S06 Home: header only for now; 03-04 adds the hero, grid, sheet and exit dialog. */
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
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
    }
}
