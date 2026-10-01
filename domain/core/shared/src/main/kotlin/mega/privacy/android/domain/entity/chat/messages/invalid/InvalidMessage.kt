package mega.privacy.android.domain.entity.chat.messages.invalid

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Invalid message
 */
@Polymorphic
interface InvalidMessage : TypedMessage