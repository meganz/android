package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Call message
 *
 * @property content Message content
 */
@Polymorphic
interface NormalMessage : TypedMessage {
    val content: String
}