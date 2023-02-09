package mega.privacy.android.app.presentation.settings.exportrecoverykey

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.presentation.settings.exportrecoverykey.model.RecoveryKeyUIState
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
    private val _uiState = MutableStateFlow(RecoveryKeyUIState())

    /**
     * Flow of [ExportRecoveryKeyActivity] UI State
     * @see ExportRecoveryKeyActivity
     * @see RecoveryKeyUIState
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Exports the Recovery Key
     */
    suspend fun getRecoveryKey(): String? {
        return getExportMasterKey().also { key ->
            if (key.isNullOrBlank().not()) {
                setMasterKeyExported()
            }
        }
    }

    /**
     * Set Action Button Group to Vertical Orientation
     */
    fun setActionGroupVertical() = viewModelScope.launch {
        _uiState.update { it.copy(isActionGroupVertical = true) }
    }

    /**
     * Action to trigger showing SnackBar in Compose View
     */
    fun showSnackBar(message: String) = viewModelScope.launch {
        _uiState.update { it.copy(snackBarMessage = message) }
    }

    /**
     * Action to mark that SnackBar has been shown
     */
    fun setSnackBarShown() = viewModelScope.launch {
        _uiState.update { it.copy(snackBarMessage = null) }
    }
}