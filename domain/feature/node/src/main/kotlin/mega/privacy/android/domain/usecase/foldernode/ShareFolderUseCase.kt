package mega.privacy.android.domain.usecase.foldernode

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to share folder with contacts
 * @property nodeRepository [NodeRepository]
 */
class ShareFolderUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     * @param nodeIds list of Node to be shared
     * @param contactData contacts with it to be shared
     * @param accessPermission [AccessPermission]
     */
    suspend operator fun invoke(
        nodeIds: List<NodeId>,
        contactData: List<String>,
        accessPermission: AccessPermission,
    ): MoveRequestResult.ShareMovement {
        val results = supervisorScope {
            nodeIds.map { nodeId ->
                async {
                    runCatching {
                        contactData.forEach { contact ->
                            nodeRepository.shareFolder(
                                nodeId = nodeId,
                                email = contact,
                                accessPermission = accessPermission
                            )
                        }
                    }
                }
            }.awaitAll()
        }

        return MoveRequestResult.ShareMovement(
            count = nodeIds.size,
            errorCount = results.count { it.isFailure },
            nodes = nodeIds.map { it.longValue }
        )
    }
}