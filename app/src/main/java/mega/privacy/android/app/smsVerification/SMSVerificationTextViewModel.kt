package mega.privacy.android.app.smsVerification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.smsVerification.model.SmsVerificationTextState
import mega.privacy.android.domain.usecase.verification.VerifyPhoneNumber
import javax.inject.Inject

/**
 * Sms verification text view model
 *
 * @property verifyPhoneNumber
 * @property verificationTextErrorMapper
 */
class SMSVerificationTextViewModel @Inject constructor(
    private val verifyPhoneNumber: VerifyPhoneNumber,
    private val verificationTextErrorMapper: (Throwable) -> String,
) : ViewModel() {

    private val _state = MutableStateFlow<SmsVerificationTextState>(SmsVerificationTextState.Empty)
    val state = _state.asStateFlow()

    internal fun submitPin(pin: String) {
        viewModelScope.launch {
            kotlin.runCatching { verifyPhoneNumber(pin) }
                .onSuccess { _state.emit(SmsVerificationTextState.VerifiedSuccessfully) }
                .onFailure { error ->
                    _state.emit(
                        SmsVerificationTextState.Failed(
                            verificationTextErrorMapper(error)
                        )
                    )
                }
        }
    }
}