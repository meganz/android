package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import javax.inject.Inject

/**
 * Get chat images nodes use case
 */
class MonitorChatImageNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val addImageTypeUseCase: AddImageTypeUseCase,
) {
    private val nodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    /**
     * Invoke use case
     */
    operator fun invoke(chatRoomId: Long, messageIds: List<Long>): Flow<List<ImageNode>> = flow {
        emit(populateNodes(chatRoomId, messageIds))
        //TODO monitorChatMessages
    }

    private suspend fun populateNodes(chatRoomId: Long, messageIds: List<Long>): List<ImageNode> {
        val nodes = messageIds.mapNotNull { messageId ->
            photosRepository.getImageNodeFromChatMessage(chatRoomId, messageId)
                ?.let { addImageTypeUseCase(it) }
                ?.let { ChatImageFile(it, chatRoomId, messageId) }
        }

        nodesCache.clear()
        nodesCache.putAll(nodes.associateBy { it.id })

        return nodesCache.values.toList()
    }
}
