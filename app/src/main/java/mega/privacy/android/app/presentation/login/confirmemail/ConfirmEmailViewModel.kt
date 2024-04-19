package mega.privacy.android.app.presentation.login.confirmemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.account.CancelCreateAccountUseCase
import mega.privacy.android.domain.usecase.createaccount.MonitorAccountConfirmationUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [ConfirmEmailFragment]
 *
 * @property uiState View state as [ConfirmEmailUiState]
 */
@HiltViewModel
class ConfirmEmailViewModel @Inject constructor(
    private val monitorAccountConfirmationUseCase: MonitorAccountConfirmationUseCase,
    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase,
    private val cancelCreateAccountUseCase: CancelCreateAccountUseCase,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmEmailUiState())
    val uiState: StateFlow<ConfirmEmailUiState> = _uiState

    init {
        viewModelScope.launch {
            monitorAccountConfirmationUseCase().collectLatest {
                _uiState.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.Login) }
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
            runCatching { resendSignUpLinkUseCase(email = email, fullName = fullName) }
                .onSuccess { email ->
                    updateRegisteredEmail(email)
                    showSuccessSnackBar()
                }
                .onFailure { error ->
                    Timber.e("Failed to re-sent the sign up link", error)
                    if (error is MegaException) {
                        error.errorString?.let {
                            showErrorSnackBar(it)
                        }
                    }
                }
        }
    }

    internal fun cancelCreateAccount() {
        viewModelScope.launch {
            Timber.d("Cancelling the registration process")
            runCatching { cancelCreateAccountUseCase() }
                .onSuccess { email ->
                    updateRegisteredEmail(email)
                    showSuccessSnackBar()
                }
                .onFailure { error ->
                    Timber.e("Failed to cancel the registration process", error)
                    if (error is MegaException) {
                        error.errorString?.let {
                            showErrorSnackBar(it)
                        }
                    }
                }
        }
    }

    private fun updateRegisteredEmail(email: String) {
        _uiState.update { it.copy(registeredEmail = email) }
    }

    private fun showSuccessSnackBar() {
        snackBarHandler.postSnackbarMessage(
            resId = R.string.confirm_email_misspelled_email_sent,
            snackbarDuration = MegaSnackbarDuration.Long
        )
    }

    private fun showErrorSnackBar(message: String) {
        snackBarHandler.postSnackbarMessage(
            message = message,
            snackbarDuration = MegaSnackbarDuration.Long
        )
    }
}