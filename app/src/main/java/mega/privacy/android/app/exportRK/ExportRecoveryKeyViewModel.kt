package mega.privacy.android.app.exportRK

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.exportRK.model.RecoveryKeyUIState
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * View model for [ExportRecoveryKeyActivity]
 * @see [ExportRecoveryKeyActivity]
 */
@HiltViewModel
class ExportRecoveryKeyViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
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
    private fun exportRK(): String? {
        val textRK = megaApi.exportMasterKey()

        if (!isTextEmpty(textRK)) {
            megaApi.masterKeyExported(null)
        }

        return textRK
    }

    /**
     * Triggers when user clicks the copy button to copy the recovery key
     */
    fun onCopyRecoveryKey() = viewModelScope.launch {
        _uiState.emit(RecoveryKeyUIState.CopyRecoveryKey(exportRK()))
    }

    /**
     * Triggers when user finish choosing the location where to store the recovery key
     */
    fun onExportRecoveryKey() = viewModelScope.launch {
        _uiState.emit(RecoveryKeyUIState.ExportRecoveryKey(exportRK()))
    }

    /**
     * Triggers when user prints the recovery key
     */
    fun onPrintRecoveryKey() = viewModelScope.launch {
        _uiState.emit(RecoveryKeyUIState.PrintRecoveryKey)
    }
}