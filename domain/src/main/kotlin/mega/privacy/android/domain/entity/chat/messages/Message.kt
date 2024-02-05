package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.chat.messages.reactions.MessageReaction

/**
 * Message
 */
interface Message {

    /**
     * Msg id
     */
    val msgId: Long

    /**
     * Time of the message in seconds
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

    /**
     * Should show avatar
     */
    val shouldShowAvatar: Boolean

    /**
     * Should show time
     */
    val shouldShowTime: Boolean

    /**
     * Should show date
     */
    val shouldShowDate: Boolean

    /**
     * List of reactions
     */
    val reactions: List<MessageReaction>
}