package mega.privacy.android.domain.entity.call

sealed interface CallRecordingConsentStatus {
    val chatId: Long

    data object None : CallRecordingConsentStatus {
        override val chatId = -1L
    }

    data class Pending(override val chatId: Long) : CallRecordingConsentStatus
    data class Granted(override val chatId: Long) : CallRecordingConsentStatus
    data class Denied(override val chatId: Long) : CallRecordingConsentStatus

}