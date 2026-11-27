package app.palm.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.palm.BuildConfig
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneButton
import app.palm.core.designsystem.components.NocturneButtonStyle
import app.palm.core.designsystem.components.NocturneChip
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
    val caption = LocalNocturne.current.type.caption
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NocturneSpace.S8)) {
        when (val s = status) {
            BackendStatus.Checking -> NocturneChip("Backend: Checking…", dotColor = NocturneSemantic.MutedForeground)
            BackendStatus.Connected -> NocturneChip("Backend: Connected", dotColor = NocturneColors.StatusSuccess)
            is BackendStatus.Unreachable -> {
                NocturneChip("Backend: Unreachable", dotColor = NocturneColors.StatusError)
                Text(s.cause, style = caption, color = NocturneSemantic.MutedForeground)
            }
        }
        Text(BuildConfig.BASE_URL, style = caption, color = NocturneSemantic.MutedForeground)
        NocturneButton("Retry", onClick = viewModel::retry, style = NocturneButtonStyle.Ghost)
    }
}
