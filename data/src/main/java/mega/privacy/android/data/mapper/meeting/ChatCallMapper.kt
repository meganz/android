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
    private val callTermCodeMapper: CallTermCodeMapper,
    private val callCompositionChangesMapper: CallCompositionChangesMapper,
    private val networkQualityMapper: NetworkQualityMapper,
    private val chatWaitingRoomMapper: ChatWaitingRoomMapper,
    private val waitingRoomStatusMapper: WaitingRoomStatusMapper,
    private val chatSessionMapper: ChatSessionMapper
) {
    operator fun invoke(megaChatCall: MegaChatCall): ChatCall = ChatCall(
        callId = megaChatCall.callId,
        chatId = megaChatCall.chatid,
        status = chatCallStatusMapper(megaChatCall.status),
        caller = megaChatCall.caller,
        duration = megaChatCall.duration.seconds,
        numParticipants = megaChatCall.numParticipants,
        changes = chatCallChangesMapper(megaChatCall.changes),
        endCallReason = endCallReasonMapper(megaChatCall.endCallReason),
        callCompositionChange = callCompositionChangesMapper(megaChatCall.callCompositionChange),
        peerIdCallCompositionChange = megaChatCall.peeridCallCompositionChange,
        peerIdParticipants = handleListMapper(megaChatCall.peeridParticipants),
        moderators = handleListMapper(megaChatCall.moderators),
        sessionsClientId = handleListMapper(megaChatCall.sessionsClientid),
        networkQuality = networkQualityMapper(megaChatCall.networkQuality),
        termCode = callTermCodeMapper(megaChatCall.termCode),
        initialTimestamp = megaChatCall.initialTimeStamp,
        finalTimestamp = megaChatCall.finalTimeStamp,
        isAudioDetected = megaChatCall.isAudioDetected,
        hasSpeakPermission = megaChatCall.hasSpeakPermission(),
        isIgnored = megaChatCall.isIgnored,
        isIncoming = megaChatCall.isIncoming,
        isOnHold = megaChatCall.isOnHold,
        isOutgoing = megaChatCall.isOutgoing,
        isOwnClientCaller = megaChatCall.isOwnClientCaller,
        isOwnModerator = megaChatCall.isOwnModerator,
        isRinging = megaChatCall.isRinging,
        isSpeakAllowed = megaChatCall.isSpeakAllowed,
        hasLocalAudio = megaChatCall.hasLocalAudio(),
        hasLocalVideo = megaChatCall.hasLocalVideo(),
        hasPendingSpeakRequest = megaChatCall.hasPendingSpeakRequest(),
        waitingRoom = chatWaitingRoomMapper(megaChatCall.waitingRoom),
        waitingRoomStatus = waitingRoomStatusMapper(megaChatCall.wrJoiningState),
        isSpeakRequestEnabled = megaChatCall.isSpeakRequestEnabled,
        sessionByClientId = megaChatCall.toChatSessionByClientId()
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