package mega.privacy.android.app.presentation.verifytwofactor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.verifytwofactor.model.PasswordChangedAction
import mega.privacy.android.app.presentation.verifytwofactor.model.VerifyTwoFactorResult
import mega.privacy.android.app.presentation.verifytwofactor.model.VerifyTwoFactorUiState
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_MAIL_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA
import mega.privacy.android.app.utils.Constants.DISABLE_2FA
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.ConstantsUrl
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.WrongMultiFactorAuthPinException
import mega.privacy.android.domain.usecase.account.ChangePasswordWith2FAUseCase
import mega.privacy.android.domain.usecase.account.DisableMultiFactorAuthUseCase
import mega.privacy.android.domain.usecase.account.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.account.RequestChangeEmailWith2FAUseCase
import mega.privacy.android.domain.usecase.account.RequestDeleteAccountLinkWith2FAUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

private const val PIN_LENGTH = 6

/**
 * ViewModel backing [VerifyTwoFactorActivity]. Owns the PIN state and dispatches the
 * verification request to the right use case based on the `verifyType` intent extra.
 *
 * Replaces the legacy direct `MegaApi.multiFactorAuth*` calls done in the activity.
 *
 * Logout follow-up is handled at [MegaChatRequestHandler] `onRequestFinished`.
 */
@HiltViewModel
class VerifyTwoFactorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val isMultiFactorAuthEnabledUseCase: IsMultiFactorAuthEnabledUseCase,
    private val requestDeleteAccountLinkWith2FAUseCase: RequestDeleteAccountLinkWith2FAUseCase,
    private val requestChangeEmailWith2FAUseCase: RequestChangeEmailWith2FAUseCase,
    private val disableMultiFactorAuthUseCase: DisableMultiFactorAuthUseCase,
    private val changePasswordWith2FAUseCase: ChangePasswordWith2FAUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getDomainNameUseCase: GetDomainNameUseCase,
) : ViewModel() {

    private val verifyType: Int = savedStateHandle[VerifyTwoFactorActivity.KEY_VERIFY_TYPE] ?: 0
    private val newEmail: String? = savedStateHandle[VerifyTwoFactorActivity.KEY_NEW_EMAIL]
    private val newPassword: String? = savedStateHandle[VerifyTwoFactorActivity.KEY_NEW_PASSWORD]
    private val isLogout: Boolean =
        savedStateHandle[ChangePasswordActivity.KEY_IS_LOGOUT] ?: false

    val uiState: StateFlow<VerifyTwoFactorUiState>
        field: MutableStateFlow<VerifyTwoFactorUiState> = MutableStateFlow(
            VerifyTwoFactorUiState(
                verifyType = verifyType,
                recoveryUrl = ConstantsUrl.recoveryUrl(getDomainNameUseCase()),
            )
        )

    init {
        checkIs2FAEnabled()
    }

    private fun checkIs2FAEnabled() {
        viewModelScope.launch {
            runCatching { isMultiFactorAuthEnabledUseCase() }
                .onSuccess { enabled ->
                    Timber.d("2fa is enabled: $enabled")
                    uiState.update { it.copy(is2FAEnabled = enabled) }
                }
                .onFailure { Timber.w(it, "Check 2fa enable state error") }
        }
    }

    /**
     * Update the PIN state. When the field reaches 6 chars and no error is currently shown,
     * trigger the verification request automatically (preserves legacy behaviour).
     */
    fun onPinChanged(value: String) {
        val sanitized = value.take(PIN_LENGTH).filter(Char::isDigit)
        uiState.update { it.copy(pin = sanitized, isPinError = false) }
        if (sanitized.length == PIN_LENGTH) {
            submit()
        }
    }

    private fun submit() {
        val state = uiState.value
        if (state.isLoading || state.pin.length != PIN_LENGTH) return
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, isBackEnabled = false) }
            runCatching { verify(state.pin) }
                .onSuccess { onSuccess() }
                .onFailure { onFailure(it) }
            uiState.update { it.copy(isLoading = false, isBackEnabled = true) }
        }
    }

    private suspend fun verify(pin: String) {
        when (verifyType) {
            CANCEL_ACCOUNT_2FA -> requestDeleteAccountLinkWith2FAUseCase(pin)
            CHANGE_MAIL_2FA -> {
                val email = newEmail ?: return emitGenericError()
                requestChangeEmailWith2FAUseCase(email, pin)
            }

            DISABLE_2FA -> disableMultiFactorAuthUseCase(pin)
            CHANGE_PASSWORD_2FA -> {
                val password = newPassword ?: return emitGenericError()
                changePasswordWith2FAUseCase(password, pin)
            }

            else -> emitGenericError()
        }
    }

    private fun emitGenericError() {
        Timber.e("Missing intent extras for verifyType=$verifyType")
        emitResult(VerifyTwoFactorResult.GenericError(R.string.general_error_word))
    }

    private fun onSuccess() {
        when (verifyType) {
            CANCEL_ACCOUNT_2FA -> emitResult(VerifyTwoFactorResult.CancelAccountLinkSent)
            CHANGE_MAIL_2FA -> emitResult(VerifyTwoFactorResult.EmailChangeLinkSent)
            DISABLE_2FA -> {
                uiState.update { it.copy(disableSuccessEvent = triggered) }
                emitResult(VerifyTwoFactorResult.MultiFactorAuthDisabled)
            }

            CHANGE_PASSWORD_2FA -> uiState.update {
                if (isLogout) {
                    it.copy(logoutEvent = triggered)
                } else {
                    it.copy(
                        passwordChangedEvent = triggered(
                            PasswordChangedAction.NavigateToMyAccount(MegaError.API_OK)
                        )
                    )
                }
            }
        }
    }

    private fun onFailure(throwable: Throwable) {
        Timber.e(throwable, "verifyType=$verifyType verification failed")
        if (throwable is WrongMultiFactorAuthPinException) {
            if (uiState.value.is2FAEnabled) {
                uiState.update { it.copy(isPinError = true) }
            }
            return
        }
        val errorCode = (throwable as? MegaException)?.errorCode ?: Int.MIN_VALUE
        when (verifyType) {
            CHANGE_MAIL_2FA -> emitResult(mapChangeEmailFailure(errorCode))
            CANCEL_ACCOUNT_2FA, DISABLE_2FA, CHANGE_PASSWORD_2FA ->
                emitResult(VerifyTwoFactorResult.GenericError(genericErrorTitleFor(verifyType)))

            else -> emitGenericError()
        }
    }

    private fun mapChangeEmailFailure(errorCode: Int): VerifyTwoFactorResult = when (errorCode) {
        MegaError.API_EACCESS -> VerifyTwoFactorResult.EmailAlreadyInUse
        MegaError.API_EEXIST -> VerifyTwoFactorResult.EmailChangeAlreadyRequested
        else -> VerifyTwoFactorResult.GenericError(R.string.general_error_word)
    }

    private fun genericErrorTitleFor(verifyType: Int): Int = when (verifyType) {
        DISABLE_2FA -> R.string.error_disable_2fa
        CHANGE_PASSWORD_2FA -> INVALID_VALUE
        else -> R.string.general_error_word
    }

    private fun emitResult(result: VerifyTwoFactorResult) {
        uiState.update { it.copy(resultEvent = triggered(result)) }
    }

    /** Consume the result dialog event. */
    fun onResultEventConsumed() {
        uiState.update { it.copy(resultEvent = consumed()) }
    }

    /** Consume the password-changed navigation event. */
    fun onPasswordChangedEventConsumed() {
        uiState.update { it.copy(passwordChangedEvent = consumed()) }
    }

    /** Consume the logout event. */
    fun onLogoutEventConsumed() {
        uiState.update { it.copy(logoutEvent = consumed) }
    }

    /** Consume the disable-success event after the activity sets RESULT_OK. */
    fun onDisableSuccessEventConsumed() {
        uiState.update { it.copy(disableSuccessEvent = consumed) }
    }

    /**
     * Logs out the user. Navigation is handled at [MegaChatRequestHandler] `onRequestFinished`.
     */
    fun logout() {
        viewModelScope.launch {
            runCatching { logoutUseCase() }
                .onFailure { Timber.d("Error on logout $it") }
        }
    }
}
