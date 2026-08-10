package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.yield
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper to get MegaNode from chat message IDs.
 * This centralizes the logic for fetching chat nodes, ensuring consistency and proper lifecycle management.
 * Re-fetches the MegaNode when called, avoiding memory safety issues where GC-released chat messages
 * cause MegaNode to become a dangling pointer.
 */
internal class MegaNodeFromChatMessageMapper @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
) {
    /**
     * Get MegaNode from chat message IDs
     *
     * @param chatId Chat room ID
     * @param messageId Message ID
     * @param messageIndex Index of the node in message attachments (default is 0)
     * @return MegaNode from the chat message, or null if not found.
     *         Note: MegaNode will be released when chatMessage is GC'd, so it should be used immediately
     *         or re-fetched when actually needed in async operations.
     */
    suspend operator fun invoke(
        chatId: Long,
        messageId: Long,
        messageIndex: Int = 0,
    ): MegaNode? {
        return (megaChatApiGateway.getMessage(chatId, messageId)
            ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
            ?.let { messageChat ->
                yield()
                val node = messageChat.megaNodeList.get(messageIndex)
                val chat = megaChatApiGateway.getChatRoom(chatId)
                yield()

                if (chat?.isPreview == true) {
                    megaApiGateway.authorizeChatNode(node, chat.authorizationToken)
                } else {
                    node
                }
            }
    }
}

