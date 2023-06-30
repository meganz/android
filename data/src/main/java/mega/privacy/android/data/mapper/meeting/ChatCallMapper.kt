package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.entity.chat.ChatCall
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

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
) {
    operator fun invoke(megaChatCall: MegaChatCall): ChatCall = ChatCall(
        callId = megaChatCall.callId,
        chatId = megaChatCall.chatid,
        status = chatCallStatusMapper(megaChatCall.status),
        caller = megaChatCall.caller,
        duration = megaChatCall.duration,
        numParticipants = megaChatCall.numParticipants,
        changes = chatCallChangesMapper(megaChatCall.changes),
        endCallReason = endCallReasonMapper(megaChatCall.endCallReason),
        callCompositionChange = callCompositionChangesMapper(megaChatCall.callCompositionChange),
        peeridCallCompositionChange = megaChatCall.peeridCallCompositionChange,
        peerIdParticipants = handleListMapper(megaChatCall.peeridParticipants),
        moderators = handleListMapper(megaChatCall.moderators),
        sessionsClientid = handleListMapper(megaChatCall.sessionsClientid),
        networkQuality = networkQualityMapper(megaChatCall.networkQuality),
        termCode = callTermCodeMapper(megaChatCall.termCode),
        initialTimestamp = megaChatCall.initialTimeStamp,
        finalTimestamp = megaChatCall.finalTimeStamp,
        isAudioDetected = megaChatCall.isAudioDetected,
        isIgnored = megaChatCall.isIgnored,
        isIncoming = megaChatCall.isIncoming,
        isOnHold = megaChatCall.isOnHold,
        isOutgoing = megaChatCall.isOutgoing,
        isOwnClientCaller = megaChatCall.isOwnClientCaller,
        isOwnModerator = megaChatCall.isOwnModerator,
        isRinging = megaChatCall.isRinging,
        isSpeakAllow = megaChatCall.isSpeakAllow,
        hasLocalAudio = megaChatCall.hasLocalAudio(),
        hasLocalVideo = megaChatCall.hasLocalVideo(),
        hasRequestSpeak = megaChatCall.hasRequestSpeak(),
    )
}