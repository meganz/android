package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.data.mapper.HandleListMapper
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Call mapper impl
 */
internal class ChatCallMapperImpl @Inject constructor(
    private val handleListMapper: HandleListMapper,
) : ChatCallMapper {
    override fun invoke(megaChatCall: MegaChatCall) =
        ChatCall(
            callId = megaChatCall.callId,
            chatId = megaChatCall.chatid,
            status = callStatus[megaChatCall.status],
            caller = megaChatCall.caller,
            duration = megaChatCall.duration,
            numParticipants = megaChatCall.numParticipants,
            changes = callChanges[megaChatCall.changes],
            endCallReason = megaChatCall.endCallReason,
            callCompositionChange = megaChatCall.callCompositionChange,
            peeridCallCompositionChange = megaChatCall.peeridCallCompositionChange,
            peerIdParticipants = handleListMapper(megaChatCall.peeridParticipants),
            moderators = handleListMapper(megaChatCall.moderators),
            sessionsClientid = handleListMapper(megaChatCall.sessionsClientid),
            networkQuality = megaChatCall.networkQuality,
            termCode = megaChatCall.termCode,
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

    companion object {
        internal val callStatus = mapOf(
            MegaChatCall.CALL_STATUS_INITIAL to ChatCallStatus.Initial,
            MegaChatCall.CALL_STATUS_USER_NO_PRESENT to ChatCallStatus.UserNoPresent,
            MegaChatCall.CALL_STATUS_CONNECTING to ChatCallStatus.Connecting,
            MegaChatCall.CALL_STATUS_JOINING to ChatCallStatus.Joining,
            MegaChatCall.CALL_STATUS_IN_PROGRESS to ChatCallStatus.InProgress,
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION to ChatCallStatus.TerminatingUserParticipation,
            MegaChatCall.CALL_STATUS_DESTROYED to ChatCallStatus.Destroyed
        )

        internal val callChanges = mapOf(
            MegaChatCall.CHANGE_TYPE_STATUS to ChatCallChanges.Status,
            MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS to ChatCallChanges.LocalAVFlags,
            MegaChatCall.CHANGE_TYPE_RINGING_STATUS to ChatCallChanges.RingingStatus,
            MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION to ChatCallChanges.CallComposition,
            MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD to ChatCallChanges.OnHold,
            MegaChatCall.CHANGE_TYPE_CALL_SPEAK to ChatCallChanges.Speaker,
            MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL to ChatCallChanges.AudioLevel,
            MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY to ChatCallChanges.NetworkQuality,
            MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP to ChatCallChanges.OutgoingRingingStop,
        )
    }
}