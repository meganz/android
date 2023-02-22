package mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.ChangePassword
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.GetPasswordStrength
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetPassword
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

@HiltViewModel
internal class ChangePasswordViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
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

    private val linkToReset
        get() = savedStateHandle.get<String>(ChangePasswordActivity.KEY_LINK_TO_RESET)

    private val action
        get() = savedStateHandle.get<String>(ChangePasswordActivity.KEY_ACTION)

    private val masterKey
        get() = savedStateHandle.get<String>(IntentConstants.EXTRA_MASTER_KEY)

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserLoggedIn = getRootFolder() != null) }

            monitorConnectivity().collect { isConnected ->
                _uiState.update { it.copy(isConnectedToNetwork = isConnected) }
            }
        }
    }

    /**
     * Triggers password reset for the user
     * @param newPassword the new password that the user inputs
     */
    fun onExecuteResetPassword(
        newPassword: String,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingMessage = R.string.my_account_changing_password)
            }

            runCatching {
                resetPassword(
                    link = linkToReset,
                    newPassword = newPassword,
                    masterKey = masterKey
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(isPasswordReset = true, loadingMessage = null)
                }
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

    /**
     * Action to check password strength level
     * @param password the new password that the user inputs
     * will update the password UI based on the strength level
     */
    fun checkPasswordStrength(password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    passwordStrengthLevel = if (password.length < 4) MegaApiJava.PASSWORD_STRENGTH_VERYWEAK else getPasswordStrength(
                        password = password
                    ),
                    isCurrentPassword = isCurrentPassword(password = password)
                )
            }
        }
    }

    fun determineIfScreenIsResetPasswordMode() {
        if (action == Constants.ACTION_RESET_PASS_FROM_LINK || action == Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT) {
            _uiState.update {
                it.copy(
                    isShowAlertMessage = masterKey.isNullOrBlank() || linkToReset.isNullOrBlank(),
                    isResetPasswordLinkValid = linkToReset != null,
                    isResetPasswordMode = true
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

    fun onAlertMessageShown() {
        _uiState.update { it.copy(isShowAlertMessage = false) }
    }
}