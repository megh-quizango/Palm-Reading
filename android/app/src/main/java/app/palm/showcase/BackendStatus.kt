package app.palm.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.palm.BuildConfig
import app.palm.core.network.HealthClient
import app.palm.core.network.HealthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackendStatus {
    data object Checking : BackendStatus
    data object Connected : BackendStatus
    data class Unreachable(val cause: String) : BackendStatus
}

@HiltViewModel
class BackendStatusViewModel @Inject constructor(private val health: HealthClient) : ViewModel() {
    private val _state = MutableStateFlow<BackendStatus>(BackendStatus.Checking)
    val state: StateFlow<BackendStatus> = _state.asStateFlow()

    private var probe: Job? = null

    init {
        retry()
    }

    /** Single-flight: a tap while a probe is running is ignored (runs on the main thread, so no race). */
    fun retry() {
        if (probe?.isActive == true) return
        _state.value = BackendStatus.Checking
        probe = viewModelScope.launch {
            _state.value = when (val r = health.check()) {
                HealthResult.Ok -> BackendStatus.Connected
                is HealthResult.Failed -> BackendStatus.Unreachable(r.cause)
            }
        }
    }
}

@Composable
fun BackendStatusRow(viewModel: BackendStatusViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier) {
        when (val s = status) {
            BackendStatus.Checking -> Text("Backend: Checking…")
            BackendStatus.Connected -> Text("Backend: Connected")
            is BackendStatus.Unreachable -> {
                Text("Backend: Unreachable")
                Text(s.cause, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(BuildConfig.BASE_URL, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = viewModel::retry) { Text("Retry") }
    }
}
