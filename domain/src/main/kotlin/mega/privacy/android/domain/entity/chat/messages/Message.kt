package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
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
     * True if the message is deletable
     */
    val isDeletable: Boolean

    /**
     * True if the message is editable
     */
    val isEditable: Boolean

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
     * List of reactions
     */
    val reactions: List<Reaction>

    /**
     * Status
     */
    val status: ChatMessageStatus

    /**
     * Content
     */
    val content: String?

    /**
     * Whether the message content exists
     */
    val exists: Boolean get() = true

    /**
     * @return true if the message has not been sent due to an error
     */
    fun isSendError() = isMine &&
            (status == ChatMessageStatus.SENDING_MANUAL || status == ChatMessageStatus.SERVER_REJECTED)

    /**
     * @return true if the message has not been sent due to an error or another reason
     */
    fun isNotSent() = isSendError()
}