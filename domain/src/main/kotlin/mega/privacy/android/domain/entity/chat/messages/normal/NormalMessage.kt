package mega.privacy.android.domain.entity.chat.messages.normal

import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Call message
 *
 * @property tempId Temporal id replacing [msgId] when the message is not confirmed yet by the server.
 */
interface NormalMessage : TypedMessage {
    val tempId: Long
}