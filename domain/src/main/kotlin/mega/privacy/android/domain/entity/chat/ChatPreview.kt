package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Chat preview
 *
 * @property request
 * @property exist
 */
data class ChatPreview(val request: ChatRequest, val exist: Boolean)