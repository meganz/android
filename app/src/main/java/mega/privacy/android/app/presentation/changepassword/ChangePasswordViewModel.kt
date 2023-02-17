package mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.ChangePassword
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.GetPasswordStrength
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetPassword
import javax.inject.Inject

@HiltViewModel
internal class ChangePasswordViewModel @Inject constructor(
    private val monitorConnectivity: MonitorConnectivity,
    private val isCurrentPassword: IsCurrentPassword,
    private val getPasswordStrength: GetPasswordStrength,
    private val changePassword: ChangePassword,
    private val resetPassword: ResetPassword,
    private val getRootFolder: GetRootFolder,
    private val multiFactorAuthSetting: FetchMultiFactorAuthSetting,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUIState())

    /**
     * Flow of [ChangePasswordActivity] UI State
     * @see ChangePasswordActivity
     * @see ChangePasswordUIState
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserLoggedIn = getRootFolder() != null) }

            monitorConnectivity().collect { isConnected ->
                _uiState.update { it.copy(isConnectedToNetwork = isConnected) }
            }
        }
    }

    /**
     * Checks whether device is connected to network
     * @return true when currently device has network connection, else false
     * only true during this time, network connection status may change because of how flow works.
     */
    fun isConnectedToNetwork(): Boolean = uiState.value.isConnectedToNetwork

    /**
     * Triggers password reset for the user
     * @param link the reset link the user previously clicks to reset this account's password
     * @param newPassword the new password that the user inputs
     * @param masterKey the master key needed to reset this account's password
     */
    fun onExecuteResetPassword(
        link: String?,
        newPassword: String,
        masterKey: String?,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingMessage = R.string.my_account_changing_password)
            }

            runCatching {
                resetPassword(link, newPassword, masterKey)
            }.onSuccess {
                _uiState.update { it.copy(isPasswordReset = true, loadingMessage = null) }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isPasswordReset = true,
                        errorCode = (t as? MegaException)?.errorCode,
                        loadingMessage = null
                    )
                }
            }
        }
    }

    /**
     * Trigger password change for this account
     * this checks for multi-factor auth
     * if it's enabled, then the user must successfully enter the correct passcode
     * if it's disabled, then password change can proceed
     * @param newPassword the new password that the user inputs
     */
    fun onUserClickChangePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingMessage = R.string.my_account_changing_password)
            }

            multiFactorAuthSetting().also { isEnabled ->
                if (isEnabled) {
                    _uiState.update {
                        it.copy(
                            isPromptedMultiFactorAuth = true,
                            loadingMessage = null
                        )
                    }
                } else {
                    onExecuteChangePassword(newPassword)
                }
            }
        }
    }

    /**
     * Trigger password change for this account
     * @param newPassword the new password that the user inputs
     * when user has passed the multi-factor auth check, this method trigger changing the user's password
     */
    private suspend fun onExecuteChangePassword(newPassword: String) {
        changePassword(newPassword).also { isSuccess ->
            _uiState.update {
                if (isSuccess) {
                    it.copy(
                        isPasswordChanged = true,
                        loadingMessage = null
                    )
                } else {
                    it.copy(
                        snackBarMessage = R.string.general_text_error,
                        loadingMessage = null
                    )
                }
            }
        }
    }

    fun checkPasswordStrength(password: String, shouldValidatePassword: Boolean = false) {
        viewModelScope.launch {
            val isCurrentPassword =
                if (shouldValidatePassword) isCurrentPassword(password = password) else false

            _uiState.update {
                it.copy(
                    passwordStrengthLevel = getPasswordStrength(password = password),
                    isCurrentPassword = isCurrentPassword
                )
            }
        }
    }

    /**
     * Reset state when multi-factor auth has been shown to the user
     */
    fun onMultiFactorAuthShown() {
        _uiState.update { it.copy(isPromptedMultiFactorAuth = false) }
    }

    /**
     * Reset state when snack bar has been shown to the user
     */
    fun onSnackBarShown() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    /**
     * Reset state when password has been successfully changed
     */
    fun onPasswordChanged() {
        _uiState.update { it.copy(isPasswordChanged = false) }
    }

    /**
     * Reset state when password has been successfully reset
     */
    fun onPasswordReset() {
        _uiState.update { it.copy(isPasswordReset = false, errorCode = null) }
    }
}