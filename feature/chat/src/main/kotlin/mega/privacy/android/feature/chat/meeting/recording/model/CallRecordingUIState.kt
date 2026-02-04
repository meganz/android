package mega.privacy.android.feature.chat.meeting.recording.model

import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
import mega.privacy.android.domain.entity.call.CallRecordingEvent

/**
 * UI state for call recording
 *
 * @property callRecordingEvent [mega.privacy.android.domain.entity.call.CallRecordingEvent]
 * @property isRecordingConsentAccepted True if the recording consent is accepted.
 * @property isParticipatingInCall True if the user is participating in call.
 */
data class CallRecordingUIState(
    val callRecordingEvent: CallRecordingEvent = CallRecordingEvent(),
    val callRecordingConsentStatus: CallRecordingConsentStatus = CallRecordingConsentStatus.None,
    val isParticipatingInCall: Boolean = false,
) {
    /**
     * True if the session is being recorded.
     */
    val isSessionOnRecording = callRecordingEvent.isSessionOnRecording

    val isRecordingConsentAccepted: Boolean? = when (callRecordingConsentStatus) {
        is CallRecordingConsentStatus.Denied -> false
        is CallRecordingConsentStatus.Granted -> true
        CallRecordingConsentStatus.None -> null
        is CallRecordingConsentStatus.Pending -> null
    }

    /**
     * The participant who is recording or recorded the session if any.
     */
    val participantRecording = callRecordingEvent.participantRecording

    /**
     * Requires recording consent
     */
    val requiresRecordingConsent =
        isSessionOnRecording && isRecordingConsentAccepted == null && isParticipatingInCall
}