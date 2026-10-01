package mega.privacy.android.domain.entity.chat.messages.management

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Call message
 */
@Polymorphic
interface ManagementMessage : TypedMessage
