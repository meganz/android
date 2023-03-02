package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus

/**
 * Chat call
 *
 * @property callId
 * @property chatId
 * @property status
 * @property caller
 * @property duration
 * @property numParticipants
 * @property changes
 * @property endCallReason
 * @property callCompositionChange
 * @property peeridCallCompositionChange
 * @property peerIdParticipants
 * @property moderators
 * @property sessionsClientid
 * @property networkQuality
 * @property termCode
 * @property initialTimestamp
 * @property finalTimestamp
 * @property isAudioDetected
 * @property isIgnored
 * @property isIncoming
 * @property isOnHold
 * @property isOutgoing
 * @property isOwnClientCaller
 * @property isOwnModerator
 * @property isRinging
 * @property isSpeakAllow
 * @property hasLocalAudio
 * @property hasLocalVideo
 * @property hasRequestSpeak
 */
data class ChatCall(
    val callId: Long,
    val chatId: Long,
    val status: ChatCallStatus? = null,
    val caller: Long? = null,
    val duration: Long? = null,
    val numParticipants: Int? = null,
    val changes: ChatCallChanges? = null,
    val endCallReason: Int? = null,
    val callCompositionChange: Int? = null,
    val peeridCallCompositionChange: Long? = null,
    val peerIdParticipants: List<Long>? = null,
    val moderators: List<Long>? = null,
    val sessionsClientid: List<Long>? = null,
    val networkQuality: Int? = null,
    val termCode: Int? = null,
    val initialTimestamp: Long? = null,
    val finalTimestamp: Long? = null,
    val isAudioDetected: Boolean = false,
    val isIgnored: Boolean = false,
    val isIncoming: Boolean = false,
    val isOnHold: Boolean = false,
    val isOutgoing: Boolean = false,
    val isOwnClientCaller: Boolean = false,
    val isOwnModerator: Boolean = false,
    val isRinging: Boolean = false,
    val isSpeakAllow: Boolean = false,
    val hasLocalAudio: Boolean = false,
    val hasLocalVideo: Boolean = false,
    val hasRequestSpeak: Boolean = false,
)
