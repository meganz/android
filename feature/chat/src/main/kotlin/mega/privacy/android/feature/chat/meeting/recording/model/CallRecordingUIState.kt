package mega.privacy.android.feature.chat.meeting.recording.model

import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus

/**
 * Call recording ui state
 *
 * @property isSessionOnRecording
 * @property participantRecording
 * @property callRecordingConsentStatus
 * @property isParticipatingInCall
 */
data class CallRecordingUIState(
    val isSessionOnRecording: Boolean = false,
    val participantRecording: String? = null,
    val callRecordingConsentStatus: CallRecordingConsentStatus = CallRecordingConsentStatus.None,
    val isParticipatingInCall: Boolean = false,
) {
    val isRecordingConsentAccepted: Boolean? = when (callRecordingConsentStatus) {
        is CallRecordingConsentStatus.Denied -> false
        is CallRecordingConsentStatus.Granted -> true
        CallRecordingConsentStatus.None -> null
        is CallRecordingConsentStatus.Pending -> null
        is CallRecordingConsentStatus.Requested -> null
    }

    /**
     * Requires recording consent
     */
    val requiresRecordingConsent =
        isSessionOnRecording && isRecordingConsentAccepted == null && isParticipatingInCall
}