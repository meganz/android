package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.UserMessage

/**
 * Call message
 *
 * @property isEdited Whether the message is edited
 */
@Polymorphic
interface NormalMessage : UserMessage {
    val isEdited: Boolean
}