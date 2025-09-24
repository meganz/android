package mega.privacy.android.app.appstate.global.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface BlockedState : Parcelable {
    data class NotBlocked(val session: String?) : BlockedState
    data class Copyright(val text: String) : BlockedState
    data class TermsOfService(val text: String) : BlockedState
    data class BusinessAccountDisabled(val text: String) : BlockedState
    data class BusinessAccountRemoved(val text: String) : BlockedState
    data class SmsVerificationRequired(val session: String?, val text: String) : BlockedState
    data class EmailVerificationRequired(val session: String?, val text: String) : BlockedState
}