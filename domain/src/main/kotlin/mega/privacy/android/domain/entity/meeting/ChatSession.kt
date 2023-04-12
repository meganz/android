package mega.privacy.android.domain.entity.meeting

/**
 * Chat session
 *
 * Domain model corresponds to MegaChatSession
 * ChatSessionMapper converts MegaChatSession -> ChatSession
 *
 * @property changes Chat session changes
 * @property isAudioDetected Checks if audio is detected
 * @property clientId Client Id
 * @property isOnHold Checks if call is on hold
 * @property status Chat session status
 * @property isHiResCamera Checks if camera has high resolution
 * @property isHiResScreenShare Checks if screen share is in high resolution
 * @property isHiResVideo Checks if video in high resolution
 * @property isLowResCamera Checks if camera has low resolution
 * @property isLowResScreenShare Checks if screen share is in low resolution
 * @property isLowResVideo Checks if video in low resolution
 * @property isModerator Checks if user is the moderator of the meeting
 * @property peerId Peer id
 * @property termCode Chat session termination code
 */
data class ChatSession(
    val changes: ChatSessionChanges,
    val isAudioDetected: Boolean,
    val clientId: Long,
    val isOnHold: Boolean,
    val status: ChatSessionStatus,
    val isHiResCamera: Boolean,
    val isHiResScreenShare: Boolean,
    val isHiResVideo: Boolean,
    val isLowResCamera: Boolean,
    val isLowResScreenShare: Boolean,
    val isLowResVideo: Boolean,
    val isModerator: Boolean,
    val peerId: Long,
    val termCode: ChatSessionTermCode,
)