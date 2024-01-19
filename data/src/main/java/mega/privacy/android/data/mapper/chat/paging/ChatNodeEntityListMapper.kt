package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.domain.entity.node.Node
import javax.inject.Inject

/**
 * Chat node entity list mapper
 *
 * @constructor Create empty Chat node entity list mapper
 */
class ChatNodeEntityListMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param messageId
     * @param nodes
     */
    operator fun invoke(
        messageId: Long,
        nodes: List<Node>,
    ) = nodes.map {
        ChatNodeEntity(
            id = it.id,
            messageId = messageId,
            name = it.name,
            parentId = it.parentId,
            base64Id = it.base64Id,
            label = it.label,
            isFavourite = it.isFavourite,
            exportedData = it.exportedData,
            isTakenDown = it.isTakenDown,
            isIncomingShare = it.isIncomingShare,
            isNodeKeyDecrypted = it.isNodeKeyDecrypted,
            creationTime = it.creationTime,
            serializedData = it.serializedData,
            isAvailableOffline = it.isAvailableOffline,
            versionCount = it.versionCount,
        )
    }
}