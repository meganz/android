package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.meeting.CallCompositionChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatWaitingRoom
import mega.privacy.android.domain.entity.meeting.EndCallReason
import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import mega.privacy.android.domain.entity.meeting.TermCodeType
import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import java.time.Instant

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
 * @property isSpeakAllowed
 * @property hasLocalAudio
 * @property hasLocalVideo
 * @property hasPendingSpeakRequest
 * @property waitingRoom
 * @property waitingRoomStatus
 */
data class ChatCall(
    val callId: Long,
    val chatId: Long,
    val status: ChatCallStatus? = null,
    val caller: Long? = null,
    val duration: Long? = null,
    val numParticipants: Int? = null,
    val changes: List<ChatCallChanges>? = null,
    val endCallReason: EndCallReason? = null,
    val callCompositionChange: CallCompositionChanges? = null,
    val peeridCallCompositionChange: Long? = null,
    val peerIdParticipants: List<Long>? = emptyList(),
    val moderators: List<Long>? = emptyList(),
    val sessionsClientid: List<Long>? = emptyList(),
    val networkQuality: NetworkQualityType? = null,
    val termCode: TermCodeType? = null,
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
    val isSpeakAllowed: Boolean = false,
    val hasLocalAudio: Boolean = false,
    val hasLocalVideo: Boolean = false,
    val hasPendingSpeakRequest: Boolean = false,
    val waitingRoom: ChatWaitingRoom? = null,
    val waitingRoomStatus: WaitingRoomStatus? = null,
) {

    /**
     * Get call start timestamp based on current duration
     *
     * @return  Timestamp in Epoch seconds
     */
    fun getStartTimestamp(): Long? =
        duration?.takeIf { it > 0 }?.let { Instant.now().minusSeconds(it).epochSecond }
}
