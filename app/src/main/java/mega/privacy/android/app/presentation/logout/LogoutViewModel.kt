package mega.privacy.android.app.presentation.logout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.globalmanagement.ChatLogoutHandler
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Logout view model
 */
@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val chatLogoutHandler: ChatLogoutHandler,
    private val hasOfflineFilesUseCase: HasOfflineFilesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
) : ViewModel() {

    companion object {
        private val LOGOUT_TIMEOUT = 5.seconds
    }

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
     * Enhanced logout with automatic fallback to local logout on timeout/failure
     * Note: We don't cancel the network logout as localLogout() will auto-resolve it
     */
    fun logout() = viewModelScope.launch {
        _state.emit(LogoutState.Loading)

        // Start network logout without cancelling it
        val networkLogoutJob = launch {
            runCatching {
                logoutUseCase()
            }.onSuccess {
                Timber.d("Network logout succeeded")
                _state.emit(LogoutState.Success)
            }.onFailure { error ->
                Timber.d("Network logout failed: $error")
                // Don't emit error here as local logout might still succeed
            }
        }

        // Wait for network logout with delay, then fallback to local logout
        delay(LOGOUT_TIMEOUT)

        // Check if network logout completed successfully
        if (_state.value != LogoutState.Success) {
            Timber.d("Network logout timed out, calling local logout")

            // Call local logout - this will auto-resolve the ongoing network logout
            runCatching {
                chatLogoutHandler.handleChatLogout(isLoggingIn = false)
            }.onSuccess {
                Timber.d("Local logout succeeded, network logout will be auto-resolved")
                _state.emit(LogoutState.Success)
            }.onFailure { localError ->
                Timber.e(localError, "Local logout failed")
                _state.emit(LogoutState.Error)
            }
        }
    }
}

