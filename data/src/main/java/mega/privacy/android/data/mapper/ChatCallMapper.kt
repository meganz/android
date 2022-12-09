package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatCall
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaHandleList

/**
 * Mapper to convert [MegaChatCall] to [ChatCall]
 */
typealias ChatCallMapper = (@JvmSuppressWildcards MegaChatCall) -> @JvmSuppressWildcards ChatCall

internal fun toChatCall(megaChatCall: MegaChatCall): ChatCall =
    ChatCall(
        megaChatCall.callId,
        megaChatCall.chatid,
        megaChatCall.status,
        megaChatCall.caller,
        megaChatCall.duration,
        megaChatCall.numParticipants,
        megaChatCall.changes,
        megaChatCall.endCallReason,
        megaChatCall.callCompositionChange,
        megaChatCall.peeridCallCompositionChange,
        megaChatCall.peeridParticipants.mapToHandleList(),
        megaChatCall.moderators.mapToHandleList(),
        megaChatCall.sessionsClientid.mapToHandleList(),
        megaChatCall.networkQuality,
        megaChatCall.termCode,
        megaChatCall.initialTimeStamp,
        megaChatCall.finalTimeStamp,
        megaChatCall.isAudioDetected,
        megaChatCall.isIgnored,
        megaChatCall.isIncoming,
        megaChatCall.isOnHold,
        megaChatCall.isOutgoing,
        megaChatCall.isOwnClientCaller,
        megaChatCall.isOwnModerator,
        megaChatCall.isRinging,
        megaChatCall.isSpeakAllow,
        megaChatCall.hasLocalAudio(),
        megaChatCall.hasLocalVideo(),
        megaChatCall.hasRequestSpeak(),
    )

private fun MegaHandleList.mapToHandleList(): List<Long> =
    mutableListOf<Long>().apply {
        for (i in 0..size) {
            add(this@mapToHandleList.get(i.toLong()))
        }
    }
