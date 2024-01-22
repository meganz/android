package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.ensureActive
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

internal class MegaNodeMapper @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
) {
    suspend operator fun invoke(typedNode: TypedNode) = when (typedNode) {
        is ChatFile -> {
            (megaChatApiGateway.getMessage(typedNode.chatId, typedNode.messageId)
                ?: megaChatApiGateway.getMessageFromNodeHistory(
                    typedNode.chatId,
                    typedNode.messageId
                ))?.let { messageChat ->
                val node = messageChat.megaNodeList.get(typedNode.messageIndex)
                val chat = megaChatApiGateway.getChatRoom(typedNode.chatId)

                if (chat?.isPreview == true) {
                    megaApiGateway.authorizeChatNode(node, chat.authorizationToken)
                } else {
                    node
                }
            }
        }

        is PublicLinkFile -> {
            typedNode.node.serializedData?.let { serializedData ->
                megaApiGateway.unSerializeNode(serializedData)
            } ?: getPublicLink(typedNode.id)
        }

        is PublicLinkFolder -> getPublicLink(typedNode.id)

        is TypedFileNode, is TypedFolderNode -> megaApiGateway.getMegaNodeByHandle(typedNode.id.longValue)

        else -> throw IllegalStateException("Invalid type")
    }

    private suspend fun getPublicLink(nodeId: NodeId) =
        megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            coroutineContext.ensureActive()
            megaApiFolderGateway.authorizeNode(it)
        }
}