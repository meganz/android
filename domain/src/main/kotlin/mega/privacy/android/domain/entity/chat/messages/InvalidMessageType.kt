package mega.privacy.android.domain.entity.chat.messages

/**
 * Invalid message
 */
enum class InvalidMessageType {

    /**
     * Invalid message format
     */
    Format,

    /**
     * Invalid message signature
     */
    Signature,

    /**
     * Unrecognizable message
     */
    Unrecognizable
}