package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Message
 */
@Polymorphic
interface Message {

    /**
     * Chat id
     */
    val chatId: Long

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
     * List of reactions
     */
    val reactions: List<Reaction>
}