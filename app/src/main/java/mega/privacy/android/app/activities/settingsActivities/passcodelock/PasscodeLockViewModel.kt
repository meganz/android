package mega.privacy.android.app.activities.settingsActivities.passcodelock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.settingsActivities.passcodelock.model.PasscodeLockUiState
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for PasscodeLockActivity
 */
@HiltViewModel
class PasscodeLockViewModel @Inject constructor(
    private val hasOfflineFilesUseCase: HasOfflineFilesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasscodeLockUiState())

    /**
     * State of Passcode Lock
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Check if there are offline files and ongoing transfers to show logout confirmation
     */
    fun checkLogoutConfirmation() {
        viewModelScope.launch {
            runCatching {
                hasOfflineFilesUseCase() to ongoingTransfersExistUseCase()
            }.onSuccess { values ->
                _uiState.update {
                    it.copy(logoutEvent = values)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Reset logout event
     */
    fun onLogoutEventConsumed() {
        _uiState.update {
            it.copy(logoutEvent = null)
        }
    }
}