package mega.privacy.android.feature.documentscanner.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the continuous scan session screen.
 * Manages camera state and coordinates with the scan session repository.
 */
@HiltViewModel
class ScanSessionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScanSessionUiState())
    val uiState: StateFlow<ScanSessionUiState> = _uiState.asStateFlow()

    /**
     * Called when camera permission is granted.
     */
    fun onCameraPermissionGranted() {
        _uiState.update { it.copy(isCameraPermissionGranted = true) }
    }

    /**
     * Called when camera permission is denied.
     */
    fun onCameraPermissionDenied() {
        _uiState.update { it.copy(isCameraPermissionGranted = false) }
    }
}

/**
 * UI state for the scan session screen.
 *
 * @property isCameraPermissionGranted Whether camera permission has been granted
 */
data class ScanSessionUiState(
    val isCameraPermissionGranted: Boolean = false,
)
