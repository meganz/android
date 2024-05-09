package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.MegaIllegalArgumentException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.CopyTypedNodeUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import javax.inject.Inject

/**
 * Export chat nodes use case
 *
 */
class ExportChatNodesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase
) {
    /**
     * Invoke
     * @param nodes List of [TypedNode] to export
     */
    suspend operator fun invoke(
        nodes: List<TypedNode>,
    ): Map<NodeId, String> {
        val myChatFolder = getMyChatsFilesFolderIdUseCase()
        return supervisorScope {
            nodes.map { node ->
                async {
                    runCatching {
                        node.id to exportNode(node, myChatFolder)
                    }
                }
            }
        }.awaitAll()
            .mapNotNull { it.getOrNull() }
            .filter { it.second.isNotEmpty() }
            .toMap()
    }

    private suspend fun exportNode(node: TypedNode, myChatFolder: NodeId): String {
        return try {
            nodeRepository.exportNode(node)
        } catch (e: Throwable) {
            if (e is MegaIllegalArgumentException) {
                // attempt to copy the node to the chat folder
                val id = copyTypedNodeUseCase(node, myChatFolder)
                exportNodeUseCase(nodeToExport = id)
            } else {
                throw e
            }
        }
    }
}