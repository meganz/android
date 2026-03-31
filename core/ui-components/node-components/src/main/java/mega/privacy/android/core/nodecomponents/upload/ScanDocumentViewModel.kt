package mega.privacy.android.core.nodecomponents.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.domain.usecase.documentscanner.isCustomScannerEnabled
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for handling scan document functionality.
 * Manages document scanner state and error handling.
 */
@HiltViewModel
class ScanDocumentViewModel @Inject constructor(
    private val scannerHandler: ScannerHandler,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanDocumentUiState())
    val uiState: StateFlow<ScanDocumentUiState> = _uiState.asStateFlow()

    /**
     * Prepares the document scanner. If the custom continuous scanner feature flag
     * is enabled, navigates to the custom scanner. Otherwise, prepares the ML Kit
     * Document Scanner from Google Play Services.
     */
    fun prepareDocumentScanner() {
        viewModelScope.launch {
            if (getFeatureFlagValueUseCase.isCustomScannerEnabled()) {
                _uiState.update { it.copy(navigateToCustomScannerEvent = triggered) }
                return@launch
            }
            runCatching {
                scannerHandler.prepareDocumentScanner()
            }.onSuccess { gmsDocumentScanner ->
                _uiState.update { it.copy(gmsDocumentScanner = gmsDocumentScanner) }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        documentScanningError = if (exception is InsufficientRAMToLaunchDocumentScanner) {
                            DocumentScanningError.InsufficientRAM
                        } else {
                            DocumentScanningError.GenericError
                        }
                    )
                }
                Timber.e(exception, "Failed to prepare document scanner")
            }
        }
    }

    /**
     * Resets the navigateToCustomScannerEvent after navigation has been handled
     */
    fun onNavigateToCustomScannerConsumed() {
        _uiState.update { it.copy(navigateToCustomScannerEvent = consumed) }
    }

    /**
     * When the system fails to open the ML Kit Document Scanner, display a generic error message
     */
    fun onDocumentScannerFailedToOpen() {
        _uiState.update { it.copy(documentScanningError = DocumentScanningError.GenericError) }
    }

    /**
     * Resets the value of gmsDocumentScanner
     */
    fun onGmsDocumentScannerConsumed() {
        _uiState.update { it.copy(gmsDocumentScanner = null) }
    }

    /**
     * Resets the value of documentScanningError
     */
    fun onDocumentScanningErrorConsumed() {
        _uiState.update { it.copy(documentScanningError = null) }
    }
}
