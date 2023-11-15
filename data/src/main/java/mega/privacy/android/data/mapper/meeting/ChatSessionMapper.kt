package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatSession
import nz.mega.sdk.MegaChatSession
import javax.inject.Inject

internal class ChatSessionMapper @Inject constructor(
    private val chatSessionChangesMapper: ChatSessionChangesMapper,
    private val chatSessionStatusMapper: ChatSessionStatusMapper,
    private val chatSessionTermCodeMapper: ChatSessionTermCodeMapper,
) {
    operator fun invoke(session: MegaChatSession) = ChatSession(
        peerId = session.peerid,
        clientId = session.clientid,
        status = chatSessionStatusMapper(session.status),
        isSpeakAllowed = session.isSpeakAllowed,
        hasAudio = session.hasAudio(),
        hasVideo = session.hasVideo(),
        isHiResVideo = session.isHiResVideo,
        isLowResVideo = session.isLowResVideo,
        hasCamera = session.hasCamera(),
        isLowResCamera = session.isLowResCamera,
        isHiResCamera = session.isHiResCamera,
        hasScreenShare = session.hasScreenShare(),
        isHiResScreenShare = session.isHiResScreenShare,
        isLowResScreenShare = session.isLowResScreenShare,
        isOnHold = session.isOnHold,
        changes = chatSessionChangesMapper(session.changes),
        termCode = chatSessionTermCodeMapper(session.termCode),
        hasPendingSpeakRequest = session.hasPendingSpeakRequest(),
        isAudioDetected = session.isAudioDetected,
        canReceiveVideoHiRes = session.canRecvVideoHiRes(),
        canReceiveVideoLowRes = session.canRecvVideoLowRes(),
        isModerator = session.isModerator,
        isRecording = session.isRecording,
        hasSpeakPermission = session.hasSpeakPermission(),
    )
}