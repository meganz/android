package mega.privacy.android.app.presentation.login.createaccount

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
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Create Account
 */
@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getPasswordStrengthUseCase: GetPasswordStrengthUseCase,
    private val isEmailValidUseCase: IsEmailValidUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val saveLastRegisteredEmailUseCase: SaveLastRegisteredEmailUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateAccountUIState())

    /**
     * Expose UI state as StateFlow
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect {
                _uiState.update { state -> state.copy(isConnected = it) }
            }
        }
    }

    internal fun onFirstNameInputChanged(firstName: String) {
        savedStateHandle[KEY_FIRST_NAME] = firstName
        _uiState.update { it.copy(isFirstNameValid = null) }
    }

    internal fun onLastNameInputChanged(lastName: String) {
        savedStateHandle[KEY_LAST_NAME] = lastName
        _uiState.update { it.copy(isLastNameValid = null) }
    }

    internal fun onEmailInputChanged(email: String) {
        savedStateHandle[KEY_EMAIL] = email
        isEmailValid()
    }

    private fun isEmailValid(): Boolean {
        val email: String? = savedStateHandle[KEY_EMAIL]
        val isEmailValid = isEmailValidUseCase(email ?: "")
        _uiState.update { it.copy(isEmailValid = isEmailValid) }
        return isEmailValid
    }

    private fun isFirstNameValid(): Boolean {
        val firstName: String? = savedStateHandle[KEY_FIRST_NAME]
        val isFirstNameValid = firstName.isNullOrBlank().not()
        _uiState.update { it.copy(isFirstNameValid = isFirstNameValid) }
        return isFirstNameValid
    }

    private fun isLastNameValid(): Boolean {
        val lastName: String? = savedStateHandle[KEY_LAST_NAME]
        val isLastNameValid = lastName.isNullOrBlank().not()
        _uiState.update { it.copy(isLastNameValid = isLastNameValid) }
        return isLastNameValid
    }

    internal fun onPasswordInputChanged(password: String) {
        savedStateHandle[KEY_PASSWORD] = password
        _uiState.update { it.copy(isPasswordValid = null) }
        validatePassword(password)
    }

    private fun validatePassword(password: String?): Boolean {
        updatePasswordStrength(password)
        validatePasswordToConfirmPassword()
        return _uiState.value.passwordStrength != PasswordStrength.INVALID && _uiState.value.passwordStrength != PasswordStrength.VERY_WEAK
    }

    private fun isPasswordValid(): Boolean {
        val isPasswordValid = validatePassword(savedStateHandle[KEY_PASSWORD])
        _uiState.update { it.copy(isPasswordValid = isPasswordValid) }
        return isPasswordValid
    }

    private fun updatePasswordStrength(password: String?) = viewModelScope.launch {
        val passwordStrength = if (password.isNullOrBlank()) {
            PasswordStrength.INVALID
        } else if (password.length > MIN_PASSWORD_LENGTH) getPasswordStrengthUseCase(password = password) else PasswordStrength.VERY_WEAK
        _uiState.update { it.copy(passwordStrength = passwordStrength) }
    }

    internal fun onConfirmPasswordInputChanged(confirmPassword: String?) {
        savedStateHandle[KEY_CONFIRM_PASSWORD] = confirmPassword
        _uiState.update { it.copy(isConfirmPasswordMatched = null) }
    }

    private fun validatePasswordToConfirmPassword() {
        runCatching {
            getInputValue(KEY_CONFIRM_PASSWORD)
        }.onSuccess { confirmPassword ->
            isConfirmPasswordValid(confirmPassword)
        }
    }

    private fun isConfirmPasswordValid(password: String?): Boolean {
        val isConfirmPasswordMatched =
            password != null && password == savedStateHandle[KEY_PASSWORD]
        _uiState.update { it.copy(isConfirmPasswordMatched = isConfirmPasswordMatched) }

        return isConfirmPasswordMatched
    }

    private fun getInputValue(key: String): String =
        savedStateHandle[key] ?: throw RuntimeException("Value is null for sign up input: $key")

    internal fun termsOfServiceAgreedChanged(isChecked: Boolean) {
        savedStateHandle[KEY_TERMS_OF_SERVICE] = isChecked
        _uiState.update { it.copy(isTermsOfServiceAgreed = isChecked) }
    }

    internal fun e2eeAgreedChanged(isChecked: Boolean) {
        savedStateHandle[KEY_E2EE] = isChecked
        _uiState.update {
            it.copy(isE2EEAgreed = isChecked)
        }
    }

    /**
     * Create account after validating all inputs and if terms are agreed and connected to network
     */
    internal fun createAccount() = viewModelScope.launch {
        //Validate all inputs
        if (areAllInputsValid().not()) return@launch

        // Check if terms are agreed
        if (areTermsAgreed().not()) {
            _uiState.update { it.copy(showAgreeToTermsEvent = triggered) }
            return@launch
        }
        // Check if connected to network
        if (_uiState.value.isConnected.not()) {
            _uiState.update { it.copy(showNoNetworkWarning = true) }
            return@launch
        }
        // Show Create Account in progress
        _uiState.update { it.copy(isAccountCreationInProgress = true) }

        // Create account
        runCatching {
            createAccountUseCase(
                getInputValue(KEY_EMAIL),
                getInputValue(KEY_PASSWORD),
                getInputValue(KEY_FIRST_NAME),
                getInputValue(KEY_LAST_NAME)
            )
        }.onFailure { e ->
            when (e) {
                is CreateAccountException.AccountAlreadyExists -> _uiState.update {
                    it.copy(
                        isAccountCreationInProgress = false,
                        createAccountStatusEvent = triggered(
                            CreateAccountStatus.AccountAlreadyExists
                        )
                    )
                }

                is CreateAccountException.Unknown -> _uiState.update {
                    it.copy(
                        isAccountCreationInProgress = false,
                        createAccountStatusEvent = triggered(
                            CreateAccountStatus.UnknownError(e.message ?: "Unknown error")
                        )
                    )
                }
            }
        }.onSuccess { credentials ->
            _uiState.update {
                it.copy(
                    isAccountCreationInProgress = false,
                    createAccountStatusEvent = triggered(CreateAccountStatus.Success(credentials))
                )
            }
        }

    }

    private fun areTermsAgreed() =
        _uiState.value.isTermsOfServiceAgreed == true && _uiState.value.isE2EEAgreed == true

    /**
     * Validate all input to create account
     *
     * @return validation state as [Boolean]
     */
    private fun areAllInputsValid(): Boolean = listOf(
        isFirstNameValid(),
        isLastNameValid(),
        isEmailValid(),
        isPasswordValid(),
        isConfirmPasswordValid(savedStateHandle[KEY_CONFIRM_PASSWORD])
    ).all { it }

    internal fun resetShowAgreeToTermsEvent() {
        _uiState.update { it.copy(showAgreeToTermsEvent = consumed) }
    }

    internal fun closeNetworkWarning() {
        _uiState.update { it.copy(showNoNetworkWarning = false) }
    }

    internal fun resetCreateAccountStatusEvent() {
        _uiState.update { it.copy(createAccountStatusEvent = consumed()) }
    }

    internal fun onCreateAccountSuccess(credentials: EphemeralCredentials) {
        saveEphemeral(credentials)
        credentials.email?.let { saveLastRegisteredEmail(it) }
    }

    private fun saveLastRegisteredEmail(email: String) {
        viewModelScope.launch {
            runCatching {
                saveLastRegisteredEmailUseCase(email)
            }.onFailure { Timber.e(it) }
        }
    }

    private fun saveEphemeral(ephemeral: EphemeralCredentials) {
        viewModelScope.launch {
            runCatching {
                clearEphemeralCredentialsUseCase()
                saveEphemeralCredentialsUseCase(ephemeral)
            }.onFailure { Timber.e(it) }
        }
    }


    companion object {
        /**
         * Key for first name
         */
        const val KEY_FIRST_NAME = "first_name"

        /**
         * Key for last name
         */
        const val KEY_LAST_NAME = "last_name"

        /**
         * Key for email
         */
        const val KEY_EMAIL = "email"

        /**
         * Key for password
         */
        const val KEY_PASSWORD = "password"

        /**
         * Key for confirm password
         */
        const val KEY_CONFIRM_PASSWORD = "confirm_password"

        /**
         * Key for terms of service
         */
        const val KEY_TERMS_OF_SERVICE = "terms_of_service"

        /**
         * Key for e2ee
         */
        const val KEY_E2EE = "e2ee"

        /**
         * Minimum password length
         */
        const val MIN_PASSWORD_LENGTH = 4
    }
}