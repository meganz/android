package mega.privacy.android.app.presentation.testpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity.Companion.KEY_IS_LOGOUT
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity.Companion.KEY_TEST_PASSWORD_MODE
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity.Companion.WRONG_PASSWORD_COUNTER
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.app.presentation.testpassword.model.TestPasswordUIState
import mega.privacy.android.domain.usecase.BlockPasswordReminderUseCase
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.IsCurrentPasswordUseCase
import mega.privacy.android.domain.usecase.NotifyPasswordCheckedUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.SkipPasswordReminderUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [TestPasswordActivity]
 */
@HiltViewModel
class TestPasswordViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getExportMasterKeyUseCase: GetExportMasterKeyUseCase,
    private val setMasterKeyExportedUseCase: SetMasterKeyExportedUseCase,
    private val isCurrentPasswordUseCase: IsCurrentPasswordUseCase,
    private val skipPasswordReminderUseCase: SkipPasswordReminderUseCase,
    private val blockPasswordReminderUseCase: BlockPasswordReminderUseCase,
    private val notifyPasswordCheckedUseCase: NotifyPasswordCheckedUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TestPasswordUIState())

    /**
     * Flow of [TestPasswordActivity] UI State
     * @see TestPasswordActivity
     * @see TestPasswordUIState
     */
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                isLogoutMode = savedStateHandle[KEY_IS_LOGOUT] ?: false,
                isUITestPasswordMode = savedStateHandle[KEY_TEST_PASSWORD_MODE] ?: false,
                wrongPasswordAttempts = savedStateHandle[WRONG_PASSWORD_COUNTER] ?: 0
            )
        }
    }

    /**
     * Check whether [password] is the same as the current password
     * @param password the password to check
     */
    fun checkForCurrentPassword(password: String) {
        viewModelScope.launch {
            val isCurrentPassword = isCurrentPasswordUseCase(password)

            _uiState.update {
                it.copy(isCurrentPassword = if (isCurrentPassword) PasswordState.True else PasswordState.False)
            }

            if (isCurrentPassword) {
                notifyPasswordReminderSucceeded()
            } else {
                _uiState.update {
                    it.copy(wrongPasswordAttempts = uiState.value.wrongPasswordAttempts + 1)
                }

                if (uiState.value.wrongPasswordAttempts == 3) {
                    _uiState.update {
                        it.copy(
                            isUserExhaustedPasswordAttempts = triggered,
                            wrongPasswordAttempts = 0
                        )
                    }
                }
            }
        }
    }

    /**
     * Notify that password has been checked and succeed
     */
    fun notifyPasswordReminderSucceeded() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = uiState.value.isLogoutMode)
            }

            runCatching {
                notifyPasswordCheckedUseCase()

                if (uiState.value.isPasswordReminderBlocked) {
                    blockPasswordReminderUseCase()
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(isUserLogout = triggered(uiState.value.isLogoutMode))
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Notify that password reminder is dismissed by the user
     */
    fun dismissPasswordReminder(shouldLogout: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = uiState.value.isLogoutMode)
            }

            runCatching {
                skipPasswordReminderUseCase()

                if (uiState.value.isPasswordReminderBlocked) {
                    blockPasswordReminderUseCase()
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(isUserLogout = triggered(shouldLogout))
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Determine if user has block future password reminder
     */
    fun setPasswordReminderBlocked(isBlocked: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isPasswordReminderBlocked = isBlocked)
            }
        }
    }

    /**
     * Set UI to test password mode layout
     */
    fun switchToTestPasswordLayout() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUITestPasswordMode = true)
            }
        }
    }

    /**
     * Exports the Recovery Key
     */
    suspend fun getRecoveryKey(): String? {
        return getExportMasterKeyUseCase().also { key ->
            if (key.isNullOrBlank().not()) {
                setMasterKeyExportedUseCase()
            } else {
                _uiState.update { it.copy(userMessage = triggered(R.string.general_text_error)) }
                return@also
            }

            _uiState.update {
                it.copy(
                    isFinishedCopyingRecoveryKey = triggered(true),
                    userMessage = if (uiState.value.isLogoutMode) triggered(R.string.copy_MK_confirmation) else consumed()
                )
            }
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
     * Resets [uiState] isFinishedCopyingRecoveryKey state to default
     */
    fun resetFinishedCopyingRecoveryKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFinishedCopyingRecoveryKey = consumed()) }
        }
    }

    /**
     * Reset and notify that user message has been consumed by the subscriber
     */
    fun resetUserMessage() {
        viewModelScope.launch {
            _uiState.update { it.copy(userMessage = consumed()) }
        }
    }

    /**
     * Reset and notify that user logout status has been consumed by the subscriber
     */
    fun resetUserLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserLogout = consumed()) }
        }
    }

    /**
     * Reset and notify that wrong password attempts status has been consumed by the subscriber
     */
    fun resetPasswordAttemptsState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserExhaustedPasswordAttempts = consumed) }
        }
    }

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    fun logout() = viewModelScope.launch {
        MegaApplication.isLoggingOut = true
        runCatching {
            logoutUseCase()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }
}