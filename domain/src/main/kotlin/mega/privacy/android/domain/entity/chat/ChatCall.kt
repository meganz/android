package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.meeting.CallCompositionChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSession
import mega.privacy.android.domain.entity.meeting.ChatWaitingRoom
import mega.privacy.android.domain.entity.meeting.EndCallReason
import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import mega.privacy.android.domain.entity.meeting.TermCodeType
import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import java.time.Instant
import kotlin.time.Duration

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
 * @property peerIdCallCompositionChange
 * @property peerIdParticipants
 * @property moderators
 * @property sessionsClientId
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
 * @property isSpeakAllowed
 * @property hasLocalAudio
 * @property hasLocalVideo
 * @property hasPendingSpeakRequest
 * @property waitingRoom
 * @property waitingRoomStatus
 * @property hasSpeakPermission True, has speaker permission. False, if not.
 * @property isSpeakRequestEnabled True, is speak request enabled. False, if not.
 * @property sessionByClientId  List of sessions by client Id.
 */
data class ChatCall(
    val status: ChatCallStatus? = null,
    val chatId: Long,
    val callId: Long,
    val hasLocalAudio: Boolean = false,
    val hasLocalVideo: Boolean = false,
    val changes: List<ChatCallChanges>? = null,
    val hasSpeakPermission: Boolean = false,
    val isAudioDetected: Boolean = false,
    val duration: Duration? = null,
    val initialTimestamp: Long? = null,
    val finalTimestamp: Long? = null,
    val termCode: TermCodeType? = null,
    val endCallReason: EndCallReason? = null,
    val isSpeakRequestEnabled: Boolean = false,
    val isRinging: Boolean = false,
    val isOwnModerator: Boolean = false,
    val sessionsClientId: List<Long>? = emptyList(),
    val sessionByClientId: Map<Long, ChatSession> = emptyMap(),
    val peerIdCallCompositionChange: Long? = null,
    val callCompositionChange: CallCompositionChanges? = null,
    val peerIdParticipants: List<Long>? = emptyList(),
    val moderators: List<Long>? = emptyList(),
    val numParticipants: Int? = null,
    val isIgnored: Boolean = false,
    val isIncoming: Boolean = false,
    val isOutgoing: Boolean = false,
    val isOwnClientCaller: Boolean = false,
    val caller: Long? = null,
    val isOnHold: Boolean = false,
    val isSpeakAllowed: Boolean = false,
    val networkQuality: NetworkQualityType? = null,
    val hasPendingSpeakRequest: Boolean = false,
    val waitingRoomStatus: WaitingRoomStatus? = null,
    val waitingRoom: ChatWaitingRoom? = null,
) {

    /**
     * Get call start timestamp based on current duration
     *
     * @return  Timestamp in Epoch seconds
     */
    fun getStartTimestamp(): Long? =
        duration?.takeIf { it.inWholeSeconds > 0 }
            ?.let { Instant.now().minusSeconds(it.inWholeSeconds).epochSecond }
}
