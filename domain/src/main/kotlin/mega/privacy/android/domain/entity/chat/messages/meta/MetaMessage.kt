package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Meta message
 */
@Polymorphic
interface MetaMessage : TypedMessage

