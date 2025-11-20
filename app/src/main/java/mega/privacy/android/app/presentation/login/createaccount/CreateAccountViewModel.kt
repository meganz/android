package mega.privacy.android.app.presentation.login.createaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.authentication.domain.usecase.regex.DoesTextContainMixedCaseUseCase
import mega.android.authentication.domain.usecase.regex.DoesTextContainNumericUseCase
import mega.android.authentication.domain.usecase.regex.DoesTextContainSpecialCharacterUseCase
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.NAME_CHAR_LIMIT
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
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
    private val doesTextContainNumericUseCase: DoesTextContainNumericUseCase,
    private val doesTextContainMixedCaseUseCase: DoesTextContainMixedCaseUseCase,
    private val doesTextContainSpecialCharacterUseCase: DoesTextContainSpecialCharacterUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateAccountUIState())

    /**
     * Expose UI state as StateFlow
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
        monitorThemeMode()
    }

    private fun monitorThemeMode() {
        viewModelScope.launch {
            monitorThemeModeUseCase().collect { themeMode ->
                _uiState.update { it.copy(themeMode = themeMode) }
            }
        }
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
        updateFirstNameValidationState()
    }

    internal fun onLastNameInputChanged(lastName: String) {
        savedStateHandle[KEY_LAST_NAME] = lastName
        updateLastNameValidationState()
    }

    internal fun onEmailInputChanged(email: String) {
        savedStateHandle[KEY_EMAIL] = email
        isEmailValid()
    }

    /**
     * Checks the validity of the given first name.
     *
     * @param firstName The first name to validate.
     * @return A [Pair] where:
     *   - The first value is `true` if the first name is valid (not blank and does not exceed [NAME_CHAR_LIMIT]), `false` otherwise.
     *   - The second value is `true` if the first name exceeds [NAME_CHAR_LIMIT], `false` otherwise.
     *
     * The first name is considered valid if it is not blank and its length does not exceed [NAME_CHAR_LIMIT].
     * If the length exceeds [NAME_CHAR_LIMIT], the second value will be `true` to indicate the limit has been exceeded.
     */
    internal fun checkFirstNameValidity(firstName: String): Pair<Boolean, Boolean> {
        val isLengthExceeded = firstName.length > NAME_CHAR_LIMIT
        val isValid = firstName.isNotBlank() && !isLengthExceeded
        return isValid to isLengthExceeded
    }

    /**
     * Checks the validity of the given last name.
     *
     * @param lastName The last name to validate.
     * @return A [Pair] where:
     *   - The first value is `true` if the last name is valid (not blank and does not exceed [NAME_CHAR_LIMIT]), `false` otherwise.
     *   - The second value is `true` if the last name exceeds [NAME_CHAR_LIMIT], `false` otherwise.
     *
     * The last name is considered valid if it is not blank and its length does not exceed [NAME_CHAR_LIMIT].
     * If the length exceeds [NAME_CHAR_LIMIT], the second value will be `true` to indicate the limit has been exceeded.
     */
    internal fun checkLastNameValidity(lastName: String): Pair<Boolean, Boolean> {
        val isLengthExceeded = lastName.length > NAME_CHAR_LIMIT
        val isValid = lastName.isNotBlank() && !isLengthExceeded
        return isValid to isLengthExceeded
    }

    /**
     * Updates the UI state for the first name field by retrieving the current value from [savedStateHandle],
     * validating it using [checkFirstNameValidity], and updating [_uiState] accordingly.
     *
     * This function is responsible for ensuring that the UI reflects the latest validation state of the first name,
     * including whether the name is valid and whether it exceeds the allowed character limit.
     *
     * The validation logic considers a first name valid if it is not blank and does not exceed [NAME_CHAR_LIMIT].
     * If the length exceeds [NAME_CHAR_LIMIT], the UI state will indicate that the limit has been exceeded.
     *
     * This function should be called whenever the first name input changes.
     */
    private fun updateFirstNameValidationState() {
        val firstName: String = savedStateHandle[KEY_FIRST_NAME] ?: ""
        val (isValid, isLengthExceeded) = checkFirstNameValidity(firstName)
        Timber.d("CreateAccountViewModel, isValid: $isValid, isLengthExceeded: $isLengthExceeded")
        _uiState.update {
            it.copy(
                isFirstNameValid = isValid,
                isFirstNameLengthExceeded = isLengthExceeded
            )
        }
    }

    /**
     * Updates the UI state for the last name field by retrieving the current value from [savedStateHandle],
     * validating it using [checkLastNameValidity], and updating [_uiState] accordingly.
     *
     * This function ensures that the UI reflects the latest validation state of the last name,
     * including whether the name is valid and whether it exceeds the allowed character limit.
     *
     * The validation logic considers a last name valid if it is not blank and does not exceed [NAME_CHAR_LIMIT].
     * If the length exceeds [NAME_CHAR_LIMIT], the UI state will indicate that the limit has been exceeded.
     *
     * This function should be called whenever the last name input changes.
     */
    private fun updateLastNameValidationState() {
        val lastName: String = savedStateHandle[KEY_LAST_NAME] ?: ""
        val (isValid, isLengthExceeded) = checkLastNameValidity(lastName)
        _uiState.update {
            it.copy(
                isLastNameValid = isValid,
                isLastNameLengthExceeded = isLengthExceeded
            )
        }
    }

    private fun isEmailValid(): Boolean {
        val email: String = savedStateHandle[KEY_EMAIL] ?: ""
        val isEmailLengthExceeded = email.length > EMAIL_CHAR_LIMIT
        return if (isEmailLengthExceeded) {
            _uiState.update { it.copy(isEmailLengthExceeded = true) }
            false
        } else {
            val isEmailValid = isEmailValidUseCase(email)
            _uiState.update { it.copy(isEmailValid = isEmailValid, isEmailLengthExceeded = false) }
            isEmailValid
        }
    }

    private fun isFirstNameValid(): Boolean {
        val firstName: String = savedStateHandle[KEY_FIRST_NAME] ?: ""
        val (isValid, _) = checkFirstNameValidity(firstName)
        return isValid
    }

    private fun isLastNameValid(): Boolean {
        val lastName: String = savedStateHandle[KEY_LAST_NAME] ?: ""
        val (isValid, _) = checkLastNameValidity(lastName)
        return isValid
    }

    internal fun onPasswordInputChanged(password: String) = viewModelScope.launch {
        savedStateHandle[KEY_PASSWORD] = password
        _uiState.update { it.copy(isPasswordValid = null) }
        validatePassword(password)
        validatePasswordToConfirmPassword()
    }

    private suspend fun validatePassword(password: String?): Boolean {
        val passwordStrength = getPasswordStrength(password)
        _uiState.update { it.copy(passwordStrength = passwordStrength) }

        val isPasswordLengthSufficient =
            (password?.length ?: 0) >= MIN_PASSWORD_LENGTH_DESIGN_REVAMP
        val doesPasswordContainNumeric = doesTextContainNumericUseCase(password)
        val doesPasswordContainSpecialCharacter = doesTextContainSpecialCharacterUseCase(password)
        val doesPasswordContainMixedCase = doesTextContainMixedCaseUseCase(password)

        _uiState.update {
            it.copy(
                isPasswordLengthSufficient = isPasswordLengthSufficient,
                doesPasswordContainNumeric = doesPasswordContainNumeric,
                doesPasswordContainSpecialCharacter = doesPasswordContainSpecialCharacter,
                doesPasswordContainMixedCase = doesPasswordContainMixedCase
            )
        }

        return isPasswordLengthSufficient && passwordStrength !in listOf(
            PasswordStrength.INVALID,
            PasswordStrength.VERY_WEAK
        )
    }

    private suspend fun getPasswordStrength(
        password: String?,
    ): PasswordStrength {
        val passwordStrength = if (password.isNullOrBlank()) {
            PasswordStrength.INVALID
        } else getPasswordStrengthUseCase(password)

        return passwordStrength
    }

    private suspend fun isPasswordValid(): Boolean {
        val isPasswordValid = validatePassword(savedStateHandle[KEY_PASSWORD])
        _uiState.update { it.copy(isPasswordValid = isPasswordValid) }
        return isPasswordValid
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

    /**
     * Create account after validating all inputs and if terms are agreed and connected to network
     */
    internal fun createAccount() = viewModelScope.launch {
        val areAllInputsValid = areAllInputsValid()
        val areTermsAgreed = areTermsAgreed()

        if (!areTermsAgreed) {
            _uiState.update { it.copy(showAgreeToTermsEvent = triggered) }
        }
        if (!areAllInputsValid || !areTermsAgreed) return@launch

        // Check if connected to network
        if (_uiState.value.isConnected.not()) {
            _uiState.update { it.copy(showNoNetworkWarning = true) }
            return@launch
        }
        // Show Create Account in progress
        _uiState.update { it.copy(isLoading = true) }

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
                        isLoading = false,
                        createAccountStatusEvent = triggered(
                            CreateAccountStatus.AccountAlreadyExists
                        )
                    )
                }

                is CreateAccountException.Unknown -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        createAccountStatusEvent = triggered(
                            CreateAccountStatus.UnknownError(e.message ?: "Unknown error")
                        )
                    )
                }
            }
        }.onSuccess { credentials ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    createAccountStatusEvent = triggered(CreateAccountStatus.Success(credentials))
                )
            }
        }

    }

    private fun areTermsAgreed() = _uiState.value.isTermsOfServiceAgreed == true

    /**
     * Validate all input to create account
     *
     * @return validation state as [Boolean]
     */
    private suspend fun areAllInputsValid(): Boolean = listOf(
        isFirstNameValid(),
        isLastNameValid(),
        isEmailValid(),
        isPasswordValid(),
        isConfirmPasswordValid(savedStateHandle[KEY_CONFIRM_PASSWORD])
    ).all { it }

    internal fun resetShowAgreeToTermsEvent() {
        _uiState.update { it.copy(showAgreeToTermsEvent = consumed) }
    }

    internal fun networkWarningShown() {
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
        applicationScope.launch {
            runCatching {
                saveLastRegisteredEmailUseCase(email)
            }.onFailure { Timber.e(it) }
        }
    }

    private fun saveEphemeral(ephemeral: EphemeralCredentials) {
        applicationScope.launch {
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

        const val EMAIL_CHAR_LIMIT = 190

        const val NAME_CHAR_LIMIT = 40

        const val MIN_PASSWORD_LENGTH_DESIGN_REVAMP = 8
    }
}