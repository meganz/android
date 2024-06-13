package mega.privacy.android.domain.entity.meeting

import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * Chat session updated result
 *
 * @property session    [ChatSession].
 * @property call       [ChatCall]
 */
data class ChatSessionUpdatesResult(
    val session: ChatSession?,
    val call: ChatCall?,
)