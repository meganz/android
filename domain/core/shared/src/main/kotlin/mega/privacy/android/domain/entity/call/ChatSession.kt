package mega.privacy.android.domain.entity.call

/**
 * Chat session
 *
 * Domain model corresponds to MegaChatSession
 * ChatSessionMapper converts MegaChatSession -> ChatSession
 *
 * @property status                     [ChatSessionStatus].
 * @property peerId                     Peer Id.
 * @property clientId                   Client Id.
 * @property hasAudio                   True if audio flags are enabled for the session (peer is muted or not)
 * @property hasVideo                   True if video is enable, false if video is disabled
 * @property isHiResVideo               True if peer associated to this session is sending video (camera or screen share) in high resolution. False, if not.
 * @property isLowResVideo              True if peer associated to this session is sending video (camera or screen share) in low resolution. False, if not.
 * @property hasCamera                  True if peer associated to this session is sending video from camera (low or high resolution). False, if not.
 * @property isLowResCamera             True if peer associated to this session is sending video from camera in low resolution. False, if not.
 * @property isHiResCamera              True if peer associated to this session is sending video from camera in high resolution. False, if not.
 * @property hasScreenShare             True if peer associated to this session is sending video from screen share (low or high resolution). False, if not.
 * @property isHiResScreenShare         True if peer associated to this session is sending video from screen share in high resolution. False, if not.
 * @property isLowResScreenShare        True if peer associated to this session is sending video from screen share in low resolution. False, if not.
 * @property isOnHold                   If session is on hold.
 * @property changes                    [ChatSessionChanges].
 * @property termCode                   [ChatSessionTermCode].
 * @property isAudioDetected            If audio is detected for this session.
 * @property canReceiveVideoHiRes       If our client is ready to receive high resolution video from the participant of this session.
 * @property canReceiveVideoLowRes      If our client is ready to receive low resolution video from the participant of this session.
 * @property isModerator                If peer associated to the session, has moderator role in the call.
 * @property isRecording                If peer associated to the session, is recording or not the call.
 */
data class ChatSession(
    val status: ChatSessionStatus,
    val peerId: Long,
    val clientId: Long,
    val hasAudio: Boolean,
    val hasVideo: Boolean,
    val isHiResVideo: Boolean,
    val isLowResVideo: Boolean,
    val hasCamera: Boolean,
    val isLowResCamera: Boolean,
    val isHiResCamera: Boolean,
    val hasScreenShare: Boolean,
    val isHiResScreenShare: Boolean,
    val isLowResScreenShare: Boolean,
    val isOnHold: Boolean,
    val changes: List<ChatSessionChanges>? = null,
    val termCode: ChatSessionTermCode,
    val isAudioDetected: Boolean,
    val canReceiveVideoHiRes: Boolean,
    val canReceiveVideoLowRes: Boolean,
    val isModerator: Boolean,
    val isRecording: Boolean,
) {
    /**
     * Checks if the session has a change.
     *
     * @param change [ChatSessionChanges] to check.
     * @return True if the session has the change in question, false otherwise.
     */
    fun hasChanged(change: ChatSessionChanges) = changes?.contains(change) == true
}