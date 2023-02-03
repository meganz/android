package mega.privacy.android.app.exportRK

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.exportRK.model.RecoveryKeyUIState
import mega.privacy.android.domain.usecase.GetExportMasterKey
import mega.privacy.android.domain.usecase.SetMasterKeyExported
import javax.inject.Inject

/**
 * View model for [ExportRecoveryKeyActivity]
 * @see [ExportRecoveryKeyActivity]
 */
@HiltViewModel
class ExportRecoveryKeyViewModel @Inject constructor(
    private val getExportMasterKey: GetExportMasterKey,
    private val setMasterKeyExported: SetMasterKeyExported,
) : BaseRxViewModel() {
    private val _uiState = MutableSharedFlow<RecoveryKeyUIState>()

    /**
     * Flow of [ExportRecoveryKeyActivity] UI State
     * @see [ExportRecoveryKeyActivity]
     */
    val uiState = _uiState.asSharedFlow()

    /**
     * Exports the Recovery Key
     */
    private suspend fun exportRecoveryKey(): String? {
        return getExportMasterKey().also { key ->
            if (key.isNullOrBlank().not()) {
                setMasterKeyExported()
            }
        }
    }

    /**
     * Triggers when user clicks the copy button to copy the recovery key
     */
    fun onCopyRecoveryKey() = viewModelScope.launch {
        _uiState.emit(RecoveryKeyUIState.CopyRecoveryKey(exportRecoveryKey()))
    }

    /**
     * Triggers when user finish choosing the location where to store the recovery key
     */
    fun onExportRecoveryKey() = viewModelScope.launch {
        _uiState.emit(RecoveryKeyUIState.ExportRecoveryKey(exportRecoveryKey()))
    }

    /**
     * Triggers when user prints the recovery key
     */
    fun onPrintRecoveryKey() = viewModelScope.launch {
        _uiState.emit(RecoveryKeyUIState.PrintRecoveryKey)
    }
}