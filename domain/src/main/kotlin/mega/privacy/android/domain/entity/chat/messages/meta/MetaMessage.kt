package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.UserMessage

/**
 * Meta message
 * @property textMessage general text message of a meta message
 */
@Polymorphic
interface MetaMessage : UserMessage

