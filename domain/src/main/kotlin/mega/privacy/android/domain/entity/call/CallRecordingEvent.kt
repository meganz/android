package mega.privacy.android.domain.entity.call

/**
 * Call recording event
 *
 * @property isSessionOnRecording True if the session is being recorded.
 * @property participantRecording The participant who is recording or recorded the session if any.
 */
data class CallRecordingEvent(
    val isSessionOnRecording: Boolean = false,
    val participantRecording: String? = null,
)
