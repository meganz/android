package mega.privacy.android.domain.entity.chat

/**
 * Enum class for defining the chat message possible changes.
 */
enum class ChatMessageChange {

    /**
     * Status of the message changed.
     */
    STATUS,

    /**
     * Content of the message changed.
     */
    CONTENT,

    /**
     * Access to the attached nodes changed.
     */
    ACCESS,

    /**
     * Timestamp updated by chatd.
     */
    TIMESTAMP
}