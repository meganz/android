package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.ChatSessionStatus
import nz.mega.sdk.MegaChatSession
import javax.inject.Inject

/**
 * Chat session status mapper
 */
internal class ChatSessionStatusMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param status input mega chat status
     * @return [ChatSessionStatus]
     */
    operator fun invoke(status: Int) = when (status) {
        MegaChatSession.SESSION_STATUS_IN_PROGRESS -> ChatSessionStatus.Progress
        MegaChatSession.SESSION_STATUS_DESTROYED -> ChatSessionStatus.Destroyed
        else -> ChatSessionStatus.Invalid
    }
}