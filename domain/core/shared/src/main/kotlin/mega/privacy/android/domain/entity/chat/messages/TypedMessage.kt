package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Polymorphic

/**
 * Typed message - Chat message interface used by the presentation layer
 */
@Polymorphic
interface TypedMessage : Message