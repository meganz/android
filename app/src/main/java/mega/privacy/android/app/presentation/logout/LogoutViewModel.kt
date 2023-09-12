package mega.privacy.android.app.presentation.logout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Logout view model
 */
@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val hasOfflineFilesUseCase: HasOfflineFilesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
) : ViewModel() {

    private val _state: MutableStateFlow<LogoutState> = MutableStateFlow(LogoutState.Loading)
    internal val state: StateFlow<LogoutState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hasOfflineFiles = runCatching { hasOfflineFilesUseCase() }
                .getOrDefault(false)
            val ongoingTransfers = runCatching { ongoingTransfersExistUseCase() }
                .getOrDefault(false)

            if (hasOfflineFiles || ongoingTransfers) {
                _state.emit(
                    LogoutState.Data(
                        hasOfflineFiles = hasOfflineFiles,
                        hasPendingTransfers = ongoingTransfers,
                    )
                )
            } else {
                logout()
            }
        }
    }

    /**
     * Logout
     */
    fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }
}