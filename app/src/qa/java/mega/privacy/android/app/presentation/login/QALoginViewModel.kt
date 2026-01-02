package mega.privacy.android.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Login event for one-shot actions
 */
sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
}

/**
 * ViewModel for QA Login Fragment.
 * Simple login view model for testing purposes.
 */
@HiltViewModel
class QALoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(QALoginState())
    val state: StateFlow<QALoginState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    private var loginJob: Job? = null

    /**
     * Update email
     */
    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email.trim(), errorMessage = null) }
    }

    /**
     * Update password
     */
    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, errorMessage = null) }
    }

    /**
     * Perform login
     */
    fun onLoginClicked() {
        val currentState = _state.value
        val email = currentState.email.trim()
        val password = currentState.password

        if (email.isEmpty()) {
            _state.update { it.copy(errorMessage = "Email cannot be empty") }
            return
        }

        if (password.isEmpty()) {
            _state.update { it.copy(errorMessage = "Password cannot be empty") }
            return
        }

        // Cancel previous login if any
        loginJob?.cancel()

        _state.update { it.copy(isLoading = true, errorMessage = null) }

        val disableChatApiUseCase = DisableChatApiUseCase {
            MegaApplication.getInstance().disableMegaChatApi()
        }

        loginJob = viewModelScope.launch {
            // First, clean up chat resources to avoid crashes when logging in with a different account
            // This is similar to what happens in switchToAccount, but without invalidating the session
            Timber.d("Cleaning up chat resources before login: $email")
            chatLogoutUseCase(disableChatApiUseCase)

            loginUseCase(email, password, disableChatApiUseCase)
                .catch { exception ->
                    Timber.e(exception, "Login error")
                    val errorMsg = when (exception) {
                        is LoginWrongEmailOrPassword -> "Wrong email or password"
                        is LoginTooManyAttempts -> "Too many login attempts. Please try again later."
                        else -> "Login failed: ${exception.message}"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
                .collect { loginStatus ->
                    when (loginStatus) {
                        is LoginStatus.LoginStarted -> {
                            _state.update { it.copy(isLoading = true) }
                        }

                        is LoginStatus.LoginSucceed -> {
                            Timber.d("QA Login successful")
                            _state.update { it.copy(isLoading = false) }
                            _events.send(LoginEvent.NavigateToHome)
                        }

                        is LoginStatus.LoginCannotStart -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Login cannot start"
                                )
                            }
                        }

                        is LoginStatus.LoginWaiting -> {
                            // Waiting for something, keep loading
                            Timber.d("QA Login waiting: ${loginStatus.error}")
                        }

                        is LoginStatus.LoginResumed -> {
                            // Login resumed, keep loading
                            Timber.d("QA Login resumed")
                        }
                    }
                }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

/**
 * State for QA Login
 */
data class QALoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
