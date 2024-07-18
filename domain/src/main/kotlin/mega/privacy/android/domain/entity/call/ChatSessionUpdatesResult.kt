package mega.privacy.android.domain.entity.call

import mega.privacy.android.domain.entity.call.ChatCall

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