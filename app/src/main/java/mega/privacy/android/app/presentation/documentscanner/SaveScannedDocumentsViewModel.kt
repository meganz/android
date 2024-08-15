package mega.privacy.android.app.presentation.documentscanner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import javax.inject.Inject

/**
 * The [ViewModel] for Save Scanned Documents
 */
@HiltViewModel
internal class SaveScannedDocumentsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SaveScannedDocumentsUiState())

    /**
     * The Save Scanned Documents UI State
     */
    val uiState = _uiState.asStateFlow()
}