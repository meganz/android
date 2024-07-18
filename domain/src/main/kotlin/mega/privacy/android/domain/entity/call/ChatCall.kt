package mega.privacy.android.domain.entity.call

import mega.privacy.android.domain.entity.meeting.ChatWaitingRoom
import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import kotlin.time.Duration

/**
 * Chat call
 *
 * @property callId                         Id of the call.
 * @property chatId                         Id of the chat.
 * @property status                         [ChatCallStatus]
 * @property caller                         User handle of caller
 * @property changes                        List of [ChatCallChanges]
 * @property hasLocalAudio                  True if local audio flags are enabled (own peer is muted or not)
 * @property hasLocalVideo                  True if video is enable, false if video is disable
 * @property duration                       Call duration
 * @property initialTimestamp               Initial timestamp
 * @property finalTimestamp                 Final timestamp or 0 if call is in progress
 * @property isRinging                      True if the receiver of the call is aware of the call and is ringing, false otherwise.
 * @property isIgnored                      True if the call has been ignored, false otherwise.
 * @property isIncoming                     True if incoming call, false if outgoing
 * @property isOutgoing                     True if outgoing call, false if incoming
 * @property isOwnModerator                 True if our own user has moderator role in the call, false if not.
 * @property isOnHold                       True if call is on hold
 * @property isAudioDetected                True if audio is detected
 * @property isSpeakRequestEnabled          True, is speak request enabled. False, if not.
 * @property isOwnClientCaller              True if our client has started the call
 * @property usersSpeakPermission           List of users with speaker permission.
 * @property termCode                       Error or warning code for this call
 * @property endCallReason                  EndCall reason for the call
 * @property networkQuality                 Network quality
 * @property waitingRoom                    MegaChatWaitingRoom for this call, if any
 * @property waitingRoomStatus              [WaitingRoomStatus]
 * @property sessionsClientId               A list of handles with the ids of clients
 * @property sessionByClientId              List of sessions by client Id.
 * @property peerIdCallCompositionChange    Handle of the peer which has been added/removed to call
 * @property callCompositionChange          [CallCompositionChanges]
 * @property peerIdParticipants             A list of handles with the ids of peers
 * @property moderators                     A list of handles of peers that have moderator role in the call
 * @property numParticipants                Num of participants in the call
 * @property usersPendingSpeakRequest       List of users that has a pending speak request in flight
 * @property genericMessage                 Generic message string
 * @property handleList                     List that can be used for multiple purposes, or NULL in case it doesn't exists
 * @property speakersList                   List that contains the user handles of all non-moderator users that have been given speak permission
 * @property speakRequestsList              List that contains the user handles of all non-moderator users that have a pending speak request in flight
 * @property auxHandle                      Generic handle value that can be used for multiple purposes. For example with CHANGE_TYPE_LOCAL_AVFLAGS, represents the clientId of the user that MUTED our own client
 * @property callDurationLimit              The call duration limit, specified in seconds
 * @property callUsersLimit                 The call users limit
 * @property callClientsLimit               The call clients limit
 * @property callClientsPerUserLimit        The call clients limit per user
 * @property notificationType               [CallNotificationType] When a call notification is forwarded to the apps
 * @property handle                         Handle used to notify multiple events. For example in onChatCallUpdate to indicate that speak permission for a call participant has changed
 * @property flag                           A boolean used to notify multiple events. For example in onChatCallUpdate, true to indicate that speak permission for a call participant has been granted, false otherwise
 * @property callWillEndTs                  Time stamp at which the call will be ended due to restrictions
 * @property raisedHandsList                 List of users with their hands raised.
 * @property usersRaiseHands                List of users handles with raised or lowered hand
 */
data class ChatCall(
    val chatId: Long,
    val callId: Long,
    val status: ChatCallStatus? = null,
    val hasLocalAudio: Boolean = false,
    val hasLocalVideo: Boolean = false,
    val changes: List<ChatCallChanges>? = null,
    val isAudioDetected: Boolean = false,
    val usersSpeakPermission: Map<Long, Boolean> = emptyMap(),
    val usersRaiseHands: Map<Long, Boolean> = emptyMap(),
    val duration: Duration? = null,
    val initialTimestamp: Long? = null,
    val finalTimestamp: Long? = null,
    val termCode: ChatCallTermCodeType? = null,
    val callDurationLimit: Int? = null,
    val callUsersLimit: Int? = null,
    val callClientsLimit: Int? = null,
    val callClientsPerUserLimit: Int? = null,
    val endCallReason: EndCallReason? = null,
    val isSpeakRequestEnabled: Boolean = false,
    val notificationType: CallNotificationType? = null,
    val auxHandle: Long? = null,
    val isRinging: Boolean = false,
    val isOwnModerator: Boolean = false,
    val sessionByClientId: Map<Long, ChatSession> = emptyMap(),
    val sessionsClientId: List<Long>? = emptyList(),
    val peerIdCallCompositionChange: Long? = null,
    val callCompositionChange: CallCompositionChanges? = null,
    val peerIdParticipants: List<Long>? = emptyList(),
    val handle: Long? = null,
    val flag: Boolean = false,
    val moderators: List<Long>? = emptyList(),
    val raisedHandsList: List<Long>? = emptyList(),
    val numParticipants: Int? = null,
    val isIgnored: Boolean = false,
    val isIncoming: Boolean = false,
    val isOutgoing: Boolean = false,
    val isOwnClientCaller: Boolean = false,
    val caller: Long? = null,
    val isOnHold: Boolean = false,
    val genericMessage: String? = null,
    val networkQuality: NetworkQualityType? = null,
    val usersPendingSpeakRequest: Map<Long, Boolean> = emptyMap(),
    val waitingRoomStatus: WaitingRoomStatus? = null,
    val waitingRoom: ChatWaitingRoom? = null,
    val handleList: List<Long>? = emptyList(),
    val speakersList: List<Long>? = emptyList(),
    val speakRequestsList: List<Long>? = emptyList(),
    val callWillEndTs: Long? = null,
)
