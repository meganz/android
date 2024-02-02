package mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.ChangePasswordUseCase
import mega.privacy.android.domain.usecase.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsCurrentPasswordUseCase
import mega.privacy.android.domain.usecase.ResetPasswordUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ChangePasswordViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isCurrentPasswordUseCase: IsCurrentPasswordUseCase,
    private val getPasswordStrengthUseCase: GetPasswordStrengthUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val isMultiFactorAuthEnabledUseCase: IsMultiFactorAuthEnabledUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private var mJob: Job? = null

    private val _uiState = MutableStateFlow(ChangePasswordUIState())

    /**
     * Flow of [ChangePasswordActivity] UI State
     * @see ChangePasswordActivity
     * @see ChangePasswordUIState
     */
    val uiState = _uiState.asStateFlow()

    /**
     * View action whether action is Reset Password or Change Password
     */
    private val action
        get() = savedStateHandle.get<String>(ChangePasswordActivity.KEY_ACTION)

    /**
     * Reset password link if mode is reset password
     */
    private val linkToReset
        get() = savedStateHandle.get<String>(ChangePasswordActivity.KEY_LINK_TO_RESET)

    /**
     * Master key to be passed if mode is reset password
     */
    private val masterKey
        get() = savedStateHandle.get<String>(IntentConstants.EXTRA_MASTER_KEY)

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserLoggedIn = getRootNodeUseCase() != null) }

            monitorConnectivityUseCase().collect { isConnected ->
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
                resetPasswordUseCase(
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

            runCatching { isMultiFactorAuthEnabledUseCase() }.onSuccess { isEnabled ->
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
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Trigger password change for this account
     * @param newPassword the new password that the user inputs
     * when user has passed the multi-factor auth check, this method trigger changing the user's password
     */
    private suspend fun onExecuteChangePassword(newPassword: String) = runCatching {
        changePasswordUseCase(newPassword)
            .also { isSuccess ->
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
    }.onFailure {
        _uiState.update {
            it.copy(
                snackBarMessage = R.string.general_text_error,
                loadingMessage = null
            )
        }
    }

    /**
     * Action to check password strength level
     * @param password the new password that the user inputs
     * will update the password UI based on the strength level
     * previous jobs will not be evaluated when [password] value's changed
     */
    fun checkPasswordStrength(password: String) {
        mJob?.cancel()
        mJob = viewModelScope.launch {
            if (uiState.value.passwordError != null) {
                _uiState.update { it.copy(passwordError = null) }
            }

            _uiState.update {
                val isCurrentPassword =
                    if (password.length > MIN_PASSWORD_CHAR) isCurrentPasswordUseCase(password) else false
                val passwordStrengthLevel =
                    if (password.length > MIN_PASSWORD_CHAR) getPasswordStrengthUseCase(password = password) else PasswordStrength.VERY_WEAK

                if (password.isBlank()) {
                    it.copy(passwordStrength = PasswordStrength.INVALID)
                } else {
                    it.copy(
                        passwordStrength = passwordStrengthLevel,
                        isCurrentPassword = isCurrentPassword
                    )
                }
            }
        }
    }

    /**
     * Method called once during view init, to check whether view
     * is on either change password or reset password mode
     */
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
     * Resets confirm password text field error state to default null
     */
    fun validateConfirmPasswordToDefault() {
        uiState.value.confirmPasswordError?.let {
            viewModelScope.launch {
                _uiState.update { it.copy(confirmPasswordError = null) }
            }
        }
    }

    /**
     * Validate password and check for errors, will validate the error ui state
     * @param password to check and validate
     */
    fun validatePassword(password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(passwordError = getPasswordError(password))
            }
        }
    }

    /**
     * Validate all passwords and check for errors before saving
     * @param password to check and validate
     * @param confirmPassword to check and validate
     */
    fun validateAllPasswordOnSave(password: String, confirmPassword: String) {
        viewModelScope.launch {
            val passwordError =
                getPasswordError(password).takeIf { uiState.value.isResetPasswordMode.not() }
            val confirmPasswordError = getConfirmPasswordError(password, confirmPassword)

            _uiState.update {
                it.copy(
                    isSaveValidationSuccessful = passwordError == null && confirmPasswordError == null,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
        }
    }

    /**
     * Check for errors in password field
     */
    private suspend fun getPasswordError(password: String): Int? {
        return when {
            password.isEmpty() -> R.string.error_enter_password
            isCurrentPasswordUseCase(password) -> R.string.error_same_password
            getPasswordStrengthUseCase(password) <= PasswordStrength.VERY_WEAK -> R.string.error_password
            else -> null
        }
    }

    /**
     * Check for errors in confirm password field
     */
    private fun getConfirmPasswordError(password: String, confirmPassword: String): Int? {
        return when {
            confirmPassword.isBlank() -> R.string.error_enter_password
            password != confirmPassword -> R.string.error_passwords_dont_match
            else -> null
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

    /**
     * Reset state when alert message has been shown
     */
    fun onAlertMessageShown() {
        _uiState.update { it.copy(isShowAlertMessage = false) }
    }

    /**
     * Reset state when password validation is successful
     */
    fun onResetPasswordValidation() {
        _uiState.update { it.copy(isSaveValidationSuccessful = false) }
    }

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }

    companion object {
        /**
         * Minimum character count of a password to be saved
         */
        private const val MIN_PASSWORD_CHAR = 4
    }
}