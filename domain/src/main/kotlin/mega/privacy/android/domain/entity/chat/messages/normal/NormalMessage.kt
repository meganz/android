package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Call message
 */
@Polymorphic
interface NormalMessage : TypedMessage