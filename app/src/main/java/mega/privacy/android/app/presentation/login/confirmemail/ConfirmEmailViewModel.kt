package mega.privacy.android.app.presentation.login.confirmemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.login.confirmemail.mapper.ResendSignUpLinkErrorMapper
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.CancelCreateAccountUseCase
import mega.privacy.android.domain.usecase.createaccount.MonitorAccountConfirmationUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 *
 * @property uiState View state as [ConfirmEmailUiState]
 */
@HiltViewModel
class ConfirmEmailViewModel @Inject constructor(
    private val monitorAccountConfirmationUseCase: MonitorAccountConfirmationUseCase,
    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase,
    private val cancelCreateAccountUseCase: CancelCreateAccountUseCase,
    private val saveLastRegisteredEmailUseCase: SaveLastRegisteredEmailUseCase,
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val resendSignUpLinkErrorMapper: ResendSignUpLinkErrorMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmEmailUiState())
    val uiState: StateFlow<ConfirmEmailUiState> = _uiState

    init {
        viewModelScope.launch {
            monitorAccountConfirmationUseCase().collectLatest {
                _uiState.update { state -> state.copy(isAccountConfirmed = true) }
            }
        }

        viewModelScope.launch {
            monitorEphemeralCredentialsUseCase().filterNotNull().collectLatest {
                _uiState.update { state ->
                    state.copy(
                        registeredEmail = it.email,
                        firstName = it.firstName
                    )
                }
            }
        }

        viewModelScope.launch {
            monitorThemeModeUseCase().collectLatest { themeMode ->
                _uiState.update { state -> state.copy(themeMode = themeMode) }
            }
        }
    }

    /**
     * Update state with isPendingToShowFragment as null.
     */
    internal fun isPendingToShowFragmentConsumed() {
        _uiState.update { state -> state.copy(isPendingToShowFragment = null) }
    }

    /**
     * Resend the sign up link to the given email and full name
     *
     * @param email The email for the account
     * @param fullName The full name of the user
     */
    internal fun resendSignUpLink(email: String, fullName: String?) {
        viewModelScope.launch {
            Timber.d("Resending the sign-up link")
            _uiState.update { it.copy(isLoading = true) }
            runCatching { resendSignUpLinkUseCase(email = email, fullName = fullName) }
                .onSuccess { email ->
                    updateRegisteredEmail(email)
                    showSuccessSnackBar()
                }
                .onFailure { exception ->
                    Timber.e("Failed to re-send the sign up link: ${exception.message}")
                    val error = resendSignUpLinkErrorMapper(exception = exception)
                    _uiState.update { it.copy(resendSignUpLinkError = triggered(error)) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    internal fun cancelCreateAccount() {
        viewModelScope.launch {
            Timber.d("Cancelling the registration process")
            _uiState.update { it.copy(isLoading = true) }
            runCatching { cancelCreateAccountUseCase() }
                .onSuccess {
                    _uiState.update { it.copy(isCreatingAccountCancelled = true) }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to cancel the registration process")
                    if (error is MegaException) {
                        error.errorString?.let {
                            showSnackBar(it)
                        }
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Update the registered email
     */
    fun updateRegisteredEmail(email: String) {
        _uiState.update { it.copy(registeredEmail = email) }
        saveLastRegisteredEmail(email)
    }

    private fun showSuccessSnackBar() {
        _uiState.update { it.copy(shouldShowSuccessMessage = true) }
    }

    /**
     * Reset the success message visibility
     */
    internal fun onSuccessMessageDisplayed() {
        _uiState.update { it.copy(shouldShowSuccessMessage = false) }
    }

    /**
     * Show an message in a snackbar
     */
    fun showSnackBar(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    /**
     * Reset the error message
     */
    internal fun onErrorMessageDisplayed() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Consume the resend signup link error event.
     */
    internal fun onResendSignUpLinkErrorConsumed() {
        _uiState.update { it.copy(resendSignUpLinkError = consumed()) }
    }

    /**
     * Save last registered email address to local storage
     */
    internal fun saveLastRegisteredEmail(email: String) {
        viewModelScope.launch {
            runCatching {
                saveLastRegisteredEmailUseCase(email)
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Handle the cancel account creation event
     */
    fun onHandleCancelCreateAccount() {
        _uiState.update { it.copy(isCreatingAccountCancelled = false) }
    }
}
