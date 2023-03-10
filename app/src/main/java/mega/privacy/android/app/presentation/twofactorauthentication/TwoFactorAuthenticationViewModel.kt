package mega.privacy.android.app.presentation.twofactorauthentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.domain.exception.EnableMultiFactorAuthException
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.GetMultiFactorAuthCode
import mega.privacy.android.domain.usecase.IsMasterKeyExported
import javax.inject.Inject

/**
 * TwoFactorAuthenticationViewModel of the TwoFactorAuthenticationActivity
 */
@HiltViewModel
class TwoFactorAuthenticationViewModel @Inject constructor(
    private val enableMultiFactorAuth: EnableMultiFactorAuth,
    private val isMasterKeyExported: IsMasterKeyExported,
    private val getMultiFactorAuthCode: GetMultiFactorAuthCode,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorAuthenticationUIState())

    /**
     * Flow of [TwoFactorAuthenticationUIState] UI State
     */
    val uiState = _uiState.asStateFlow()


    /**
     * Get the multi factor authentication code required to enable the 2FA
     */
    fun getAuthenticationCode() {
        viewModelScope.launch {
            runCatching { getMultiFactorAuthCode() }.let { result ->
                _uiState.update {
                    it.copy(
                        seed = result.getOrNull(),
                        is2FAFetchCompleted = true
                    )
                }
            }
        }
    }

    /**
     * Get IsMasterKeyExported status after user successfully enabled 2FA
     */
    fun getMasterKeyStatus() {
        viewModelScope.launch {
            runCatching { isMasterKeyExported() }.let { result ->
                _uiState.update {
                    it.copy(
                        isMasterKeyExported = result.isSuccess,
                        dismissRecoveryKey = result.getOrNull() ?: false
                    )
                }
            }
        }
    }

    /**
     * Triggers multi factor authentication validation for the user
     * @param pin the 6 digit code required for validation process
     */
    fun submitMultiFactorAuthPin(pin: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPinSubmitted = false,
                )
            }
            runCatching {
                enableMultiFactorAuth(pin)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isPinSubmitted = true,
                        authenticationState = AuthenticationState.AuthenticationPassed,
                        dismissRecoveryKey = true
                    )
                }
                getMasterKeyStatus()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isPinSubmitted = true,
                        authenticationState =
                        if (e is EnableMultiFactorAuthException)
                            AuthenticationState.AuthenticationFailed
                        else
                            AuthenticationState.AuthenticationError,
                    )
                }
            }
        }
    }

}