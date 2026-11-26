package app.palm.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.palm.R

@Composable
fun ShowcaseScreen(statusViewModel: BackendStatusViewModel) {
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
            item { BackendStatusRow(statusViewModel) }
        }
    }
}
