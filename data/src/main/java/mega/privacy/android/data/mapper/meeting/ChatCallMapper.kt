package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatSession
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Chat call mapper
 */
internal class ChatCallMapper @Inject constructor(
    private val handleListMapper: HandleListMapper,
    private val chatCallChangesMapper: ChatCallChangesMapper,
    private val chatCallStatusMapper: ChatCallStatusMapper,
    private val endCallReasonMapper: EndCallReasonMapper,
    private val chatCallTermCodeMapper: ChatCallTermCodeMapper,
    private val callCompositionChangesMapper: CallCompositionChangesMapper,
    private val networkQualityMapper: NetworkQualityMapper,
    private val chatWaitingRoomMapper: ChatWaitingRoomMapper,
    private val waitingRoomStatusMapper: WaitingRoomStatusMapper,
    private val chatSessionMapper: ChatSessionMapper,
    private val callNotificationMapper: CallNotificationMapper,
    private val speakerStatusMapper: SpeakerStatusMapper
) {
    operator fun invoke(megaChatCall: MegaChatCall): ChatCall = ChatCall(
        callId = megaChatCall.callId,
        chatId = megaChatCall.chatid,
        status = chatCallStatusMapper(megaChatCall.status),
        hasLocalAudio = megaChatCall.hasLocalAudio(),
        hasLocalVideo = megaChatCall.hasLocalVideo(),
        changes = chatCallChangesMapper(megaChatCall.changes),
        hasSpeakPermission = megaChatCall.hasSpeakPermission(),
        isAudioDetected = megaChatCall.isAudioDetected,
        duration = megaChatCall.duration.seconds,
        initialTimestamp = megaChatCall.initialTimeStamp,
        finalTimestamp = megaChatCall.finalTimeStamp,
        termCode = chatCallTermCodeMapper(megaChatCall.termCode),
        endCallReason = endCallReasonMapper(megaChatCall.endCallReason),
        isSpeakRequestEnabled = megaChatCall.isSpeakRequestEnabled,
        notificationType = callNotificationMapper(megaChatCall.notificationType),
        auxHandle = megaChatCall.auxHandle,
        isRinging = megaChatCall.isRinging,
        isOwnModerator = megaChatCall.isOwnModerator,
        sessionsClientId = handleListMapper(megaChatCall.sessionsClientid),
        sessionByClientId = megaChatCall.toChatSessionByClientId(),
        peerIdCallCompositionChange = megaChatCall.peeridCallCompositionChange,
        callCompositionChange = callCompositionChangesMapper(megaChatCall.callCompositionChange),
        peerIdParticipants = handleListMapper(megaChatCall.peeridParticipants),
        moderators = handleListMapper(megaChatCall.moderators),
        numParticipants = megaChatCall.numParticipants,
        isIgnored = megaChatCall.isIgnored,
        isIncoming = megaChatCall.isIncoming,
        isOutgoing = megaChatCall.isOutgoing,
        isOwnClientCaller = megaChatCall.isOwnClientCaller,
        speakerState = speakerStatusMapper(megaChatCall.speakerState.toInt()),
        caller = megaChatCall.caller,
        isOnHold = megaChatCall.isOnHold,
        genericMsg = megaChatCall.genericMessage,
        isSpeakAllowed = megaChatCall.isSpeakAllowed,
        networkQuality = networkQualityMapper(megaChatCall.networkQuality),
        hasPendingSpeakRequest = megaChatCall.hasPendingSpeakRequest(),
        waitingRoomStatus = waitingRoomStatusMapper(megaChatCall.wrJoiningState),
        waitingRoom = chatWaitingRoomMapper(megaChatCall.waitingRoom),
    )

    private fun MegaChatCall.toChatSessionByClientId(): Map<Long, ChatSession> {
        val sessionsMap: MutableMap<Long, ChatSession> = mutableMapOf()
        val listOfClientId = handleListMapper(sessionsClientid)
        for (i in listOfClientId.indices) {
            val clientId = listOfClientId[i]
            getMegaChatSession(clientId)?.let { megaChatSession ->
                sessionsMap[clientId] = chatSessionMapper(megaChatSession)
            }
        }
        return sessionsMap
    }
}