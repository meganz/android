package mega.privacy.android.app.presentation.testpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.app.presentation.testpassword.model.TestPasswordUIState
import mega.privacy.android.domain.usecase.BlockPasswordReminder
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.NotifyPasswordChecked
import mega.privacy.android.domain.usecase.SkipPasswordReminder
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [TestPasswordActivity]
 */
@HiltViewModel
class TestPasswordViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val isCurrentPassword: IsCurrentPassword,
    private val skipPasswordReminder: SkipPasswordReminder,
    private val blockPasswordReminder: BlockPasswordReminder,
    private val notifyPasswordChecked: NotifyPasswordChecked,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TestPasswordUIState())

    /**
     * Flow of [TestPasswordActivity] UI State
     * @see TestPasswordActivity
     * @see TestPasswordUIState
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Check whether [password] is the same as the current password
     * @param password the password to check
     */
    fun checkForCurrentPassword(password: String) {
        viewModelScope.launch {
            val isCurrentPassword =
                if (isCurrentPassword(password)) PasswordState.True else PasswordState.False
            _uiState.update {
                it.copy(isCurrentPassword = isCurrentPassword)
            }
        }
    }

    /**
     * Notify the API that password reminder is skipped by the user
     */
    fun notifyPasswordReminderSkipped() {
        notifyAndUpdateState { skipPasswordReminder() }
    }

    /**
     * Notify the API that password reminder that password check is totally disable
     */
    fun notifyPasswordReminderBlocked() {
        notifyAndUpdateState { blockPasswordReminder() }
    }

    /**
     * Notify the API that password reminder that password reminder succeeded
     */
    fun notifyPasswordReminderSucceed() {
        notifyAndUpdateState { notifyPasswordChecked() }
    }

    private fun notifyAndUpdateState(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching {
            block()
        }.onSuccess {
            _uiState.update { it.copy(isPasswordReminderNotified = PasswordState.True) }
        }.onFailure { e ->
            _uiState.update { it.copy(isPasswordReminderNotified = PasswordState.False) }
            Timber.e("Error: MegaRequest.TYPE_SET_ATTR_USER | MegaApiJava.USER_ATTR_PWD_REMINDER ${e.message}")
        }
    }

    /**
     * Resets [uiState] isCurrentPassword state to default
     */
    fun resetCurrentPasswordState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCurrentPassword = PasswordState.Initial) }
        }
    }

    /**
     * /**
     * Resets [uiState] isPasswordReminderNotified state to default
    */
     */
    fun resetPasswordReminderState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPasswordReminderNotified = PasswordState.Initial) }
        }
    }
}