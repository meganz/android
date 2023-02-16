package mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.model.ActionResult
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
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
            monitorConnectivity().collect { isConnected ->
                _uiState.update { it.copy(isConnectedToNetwork = isConnected) }
            }
        }
    }

    /**
     * Checks whether device is connected to network
     * @return true when device has network connection, else false
     */
    fun isConnectedToNetwork(): Boolean = uiState.value.isConnectedToNetwork

    fun onConfirmResetPassword(
        link: String?,
        newPassword: String,
        masterKey: String?,
    ) {
        viewModelScope.launch {
            resetPassword(link, newPassword, masterKey).also { isSuccess ->
                _uiState.update { it.copy(isResetPassword = getActionResult(isSuccess)) }
            }
        }
    }

    fun onUserClickChangePassword(newPassword: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingMessage = R.string.my_account_changing_password
                )
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

    fun onMultiFactorAuthShown() {
        _uiState.update { it.copy(isPromptedMultiFactorAuth = false) }
    }

    fun onSnackBarShown() {
        _uiState.update { it.copy(snackBarMessage = null) }
    }

    fun onChangedPasswordShown() {
        _uiState.update { it.copy(isPasswordChanged = false) }
    }

    private fun getActionResult(isSuccess: Boolean): ActionResult =
        if (isSuccess) ActionResult.SUCCESS else ActionResult.FAILED
}