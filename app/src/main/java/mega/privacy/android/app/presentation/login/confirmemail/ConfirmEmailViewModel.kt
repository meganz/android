package mega.privacy.android.app.presentation.login.confirmemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
                .onFailure { error ->
                    Timber.e("Failed to re-sent the sign up link", error)
                    if (error is MegaException) {
                        error.errorString?.let {
                            showSnackBar(it)
                        }
                    }
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
                    Timber.e("Failed to cancel the registration process", error)
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
