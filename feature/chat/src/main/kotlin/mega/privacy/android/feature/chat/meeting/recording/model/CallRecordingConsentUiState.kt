package mega.privacy.android.feature.chat.meeting.recording.model

/**
 * Call recording consent ui state
 */
sealed class CallRecordingConsentUiState {
    /**
     * Hard coded privacy url
     */
    val privacyUrl = "https://mega.io/privacy"

    /**
     * Loading
     */
    data object Loading : CallRecordingConsentUiState()

    /**
     * Consent required
     * @param chatId chat id
     */
    data class ConsentRequired(val chatId: Long) : CallRecordingConsentUiState()

    /**
     * Consent already granted
     */
    data object ConsentAlreadyHandled : CallRecordingConsentUiState()

}