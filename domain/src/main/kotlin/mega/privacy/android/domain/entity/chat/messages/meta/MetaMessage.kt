package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Meta message
 * @property textMessage general text message of a meta message
 */
@Polymorphic
interface MetaMessage : TypedMessage

