package mega.privacy.android.domain.entity.node

/**
 * Entity sealed class for SDK calls which requires a MegaNode as param.
 * Normally used in MEGA viewers, that is why the name.
 *
 * @property id Unique identifier of a MegaNode.
 */
sealed class ViewerNode {

    abstract val id: Long

    /**
     * Node which can be get only with its id.
     */
    data class GeneralNode(override val id: Long) : ViewerNode()

    /**
     * Node coming from a file link.
     *
     * @property serializedNode String representing the serialized node.
     */
    data class FileLinkNode(
        override val id: Long,
        val serializedNode: String,
    ) : ViewerNode()

    /**
     * Node coming from a folder link. It can be get using only its id but using the
     * megaApiFolder instance instead of the megaApi one.
     */
    data class FolderLinkNode(override val id: Long) : ViewerNode()

    /**
     * Node coming from a chat conversation.
     *
     * @property chatId    Unique identifier of a chat.
     * @property messageId Unique identifier of a chat message.
     */
    data class ChatNode(
        override val id: Long,
        val chatId: Long,
        val messageId: Long,
    ) : ViewerNode()
}
