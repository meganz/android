package mega.privacy.android.domain.entity.chat.messages

/**
 * Message
 */
interface Message {

    /**
     * Msg id
     */
    val msgId: Long

    /**
     * Time of the message
     */
    val time: Long

    /**
     * True if the message is mine
     */
    val isMine: Boolean

    /**
     * User handle
     */
    val userHandle: Long
}