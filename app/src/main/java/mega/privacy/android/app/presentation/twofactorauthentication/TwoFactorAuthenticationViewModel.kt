package mega.privacy.android.app.presentation.twofactorauthentication

import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getTwoFactorAuthentication
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getUpdatedTwoFactorAuthentication
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.domain.exception.EnableMultiFactorAuthException
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.GetMultiFactorAuthCode
import mega.privacy.android.domain.usecase.IsMasterKeyExported
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import javax.inject.Inject

/**
 * TwoFactorAuthenticationViewModel of the TwoFactorAuthenticationActivity
 */
@HiltViewModel
class TwoFactorAuthenticationViewModel @Inject constructor(
    private val enableMultiFactorAuth: EnableMultiFactorAuth,
    private val isMasterKeyExported: IsMasterKeyExported,
    private val getMultiFactorAuthCode: GetMultiFactorAuthCode,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val qrCodeMapper: QRCodeMapper,
    private val getExportMasterKeyUseCase: GetExportMasterKeyUseCase,
    private val setMasterKeyExportedUseCase: SetMasterKeyExportedUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorAuthenticationUIState())

    /**
     * Flow of [TwoFactorAuthenticationUIState] UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        getMasterKeyStatus()
    }


    private fun updateTwoFAState(twoFA: List<String>) {
        _uiState.update { state ->
            state.copy(
                twoFAPin = twoFA,
                authenticationState = AuthenticationState.Fixed
                    .takeUnless { state.authenticationState == AuthenticationState.Failed }
            )
        }
    }

    /**
     * Sets isFirstTime2FA as consumed.
     */
    fun onFirstTime2FAConsumed() =
        _uiState.update { state -> state.copy(isFirstTime2FA = consumed) }

    /**
     * Updates 2FA code in state.
     */
    fun on2FAChanged(twoFA: String) = twoFA.getTwoFactorAuthentication()?.let {
        updateTwoFAState(it)
        submitMultiFactorAuthPin(twoFA)
    }

    /**
     * Updates a pin of the 2FA code in state.
     */
    fun on2FAPinChanged(pin: String, index: Int) {
        val updated2FA =
            uiState.value.twoFAPin.getUpdatedTwoFactorAuthentication(pin = pin, index = index)

        updateTwoFAState(updated2FA)
        updated2FA.getTwoFactorAuthentication()?.apply {
            submitMultiFactorAuthPin(this)
        }
    }

    /**
     * Updates the Recovery key in state
     */
    fun setIsRkExportSuccessfullyEvent(isExported: Boolean) =
        _uiState.update { it.copy(isRkExportedSuccessfullyEvent = triggered(isExported)) }

    /**
     * Sets isRkExportedSuccessfullyEvent as consumed
     */
    fun onIsRkExportSuccessfullyEventConsumed() =
        _uiState.update { it.copy(isRkExportedSuccessfullyEvent = consumed()) }

    /**
     * Updates writePermissionDeniedEvent in state
     */
    fun triggerWritePermissionDeniedEvent() =
        _uiState.update { it.copy(writePermissionDeniedEvent = triggered) }

    /**
     * Sets writePermissionDeniedEvent as consumed
     */
    fun onWritePermissionDeniedEventConsumed() =
        _uiState.update { it.copy(writePermissionDeniedEvent = consumed) }

    /**
     * Updates seedCopiedToClipboardEvent in state
     */
    fun triggerSeedCopiedToClipboardEvent() =
        _uiState.update { it.copy(seedCopiedToClipboardEvent = triggered) }

    /**
     * Sets seedCopiedToClipboardEvent as consumed
     */
    fun onSeedCopiedToClipboardEventConsumed() =
        _uiState.update { it.copy(seedCopiedToClipboardEvent = consumed) }


    /**
     * Exports the Recovery Key
     */
    suspend fun getRecoveryKey(): String? {
        return getExportMasterKeyUseCase().also { key ->
            if (key.isNullOrBlank().not()) {
                setMasterKeyExportedUseCase()
            }
        }
    }

    /**
     * Generate the QR code for the 2fa
     *
     * @param qrCodeUrl the text value of QR code.
     * @param width width of the target bitmap.
     * @param height height of the target bitmap.
     * @param penColor pen color of the QR code. Color format is ARGB.
     * @param bgColor background color of the QR code. Color format is ARGB.
     */
    fun generateQRCodeBitmap(
        qrCodeUrl: String,
        width: Int,
        height: Int,
        @ColorInt penColor: Int,
        @ColorInt bgColor: Int,
    ) {
        viewModelScope.launch {
            runCatching {
                qrCodeMapper(
                    text = qrCodeUrl,
                    width = width,
                    height = height,
                    penColor = penColor,
                    bgColor = bgColor
                ).let { bitmap ->
                    _uiState.update {
                        it.copy(
                            isQRCodeGenerationCompleted = true,
                            qrBitmap = bitmap
                        )
                    }
                }
            }
        }
    }

    /**
     * Get the current user's email
     */
    fun getUserEmail() {
        viewModelScope.launch {
            runCatching {
                getCurrentUserEmail().let { email ->
                    _uiState.update {
                        it.copy(
                            userEmail = email,
                            twoFactorAuthUrl = "otpauth://totp/MEGA:${email}?secret=${it.seed}&amp;issuer=MEGA"
                        )
                    }
                }
            }
        }
    }

    /**
     * Get the multi factor authentication code required to enable the 2FA
     */
    fun getAuthenticationCode() {
        viewModelScope.launch {
            runCatching { getMultiFactorAuthCode() }.let { result ->
                _uiState.update {
                    it.copy(
                        seed = result.getOrNull(),
                        is2FAFetchCompleted = true
                    )
                }
                getUserEmail()
            }
        }
    }

    /**
     * Get boolean state of IsMasterKeyExported of the user
     */
    fun getMasterKeyStatus() {
        viewModelScope.launch {
            runCatching { isMasterKeyExported() }.let { result ->
                _uiState.update {
                    it.copy(isMasterKeyExported = result.getOrElse { false })
                }
            }
        }
    }

    /**
     * Sets the state of the authentication pin to default state
     */
    fun on2FAPinReset() = _uiState.update {
        it.copy(
            isFirstTime2FA = triggered,
            twoFAPin = listOf("", "", "", "", "", ""),
            authenticationState = AuthenticationState.Fixed
        )
    }

    /**
     * Triggers multi factor authentication validation for the user
     * @param pin the 6 digit code required for validation process
     */
    fun submitMultiFactorAuthPin(pin: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPinSubmitted = false,
                    authenticationState = AuthenticationState.Checking
                )
            }
            runCatching {
                enableMultiFactorAuth(pin)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isPinSubmitted = true,
                        authenticationState = AuthenticationState.Passed
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isFirstTime2FA = consumed,
                        isPinSubmitted = true,
                        authenticationState =
                        if (e is EnableMultiFactorAuthException)
                            AuthenticationState.Failed
                        else
                            AuthenticationState.Error,
                    )
                }
            }
        }
    }

}