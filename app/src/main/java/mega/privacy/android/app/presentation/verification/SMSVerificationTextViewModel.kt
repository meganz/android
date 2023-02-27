package mega.privacy.android.app.presentation.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.verification.model.SmsVerificationTextState
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.domain.usecase.verification.VerifyPhoneNumber
import javax.inject.Inject

/**
 * Sms verification text view model
 *
 * @property verifyPhoneNumber
 * @property verificationTextErrorMapper
 * @property state
 */
@HiltViewModel
class SMSVerificationTextViewModel @Inject constructor(
    private val verifyPhoneNumber: VerifyPhoneNumber,
    private val verificationTextErrorMapper: SmsVerificationTextErrorMapper,
) : ViewModel() {

    private val _state = MutableStateFlow<SmsVerificationTextState>(SmsVerificationTextState.Empty)
    val state = _state.asStateFlow()

    internal fun submitPin(pin: String) {
        viewModelScope.launch {
            _state.emit(SmsVerificationTextState.Loading)
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