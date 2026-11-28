package app.palm.feature.scan

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.palm.core.photo.ProcessedCapture
import app.palm.core.photo.ScanResultHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ScanReadyViewModel @Inject constructor(private val holder: ScanResultHolder) : ViewModel() {
    val capture: ProcessedCapture? = holder.latest

    private val _bitmap = MutableStateFlow<ImageBitmap?>(null)
    val bitmap: StateFlow<ImageBitmap?> = _bitmap.asStateFlow()

    init {
        val file = capture?.file
        if (file != null) {
            viewModelScope.launch {
                _bitmap.value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
            }
        }
    }

    /** Deletes the capture (D-12). NonCancellable: the screen pops right after, which clears this scope. */
    fun retake() {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) { holder.clear() }
    }
}
