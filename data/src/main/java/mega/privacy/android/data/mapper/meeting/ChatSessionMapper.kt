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
        changes = chatSessionChangesMapper(session.changes),
        isAudioDetected = session.isAudioDetected,
        clientId = session.clientid,
        isOnHold = session.isOnHold,
        status = chatSessionStatusMapper(session.status),
        isHiResCamera = session.isHiResCamera,
        isHiResScreenShare = session.isHiResScreenShare,
        isHiResVideo = session.isHiResVideo,
        isLowResCamera = session.isLowResCamera,
        isLowResScreenShare = session.isLowResScreenShare,
        isLowResVideo = session.isLowResVideo,
        isModerator = session.isModerator,
        peerId = session.peerid,
        termCode = chatSessionTermCodeMapper(session.termCode)
    )
}