package mega.privacy.android.domain.entity.chat.messages


/**
 * User message
 *
 * Interface for messages that can be sent by a user
 */
interface UserMessage : TypedMessage {
    /**
     * Row id
     */
    val rowId: Long
}