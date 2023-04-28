package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import javax.inject.Inject

/**
 * Use Case that checks if the deleted Node is from Backups or not
 *
 * @property isNodeInRubbish [IsNodeInRubbish]
 * @property nodeRepository [NodeRepository]
 */
class IsNodeDeletedFromBackupsUseCase @Inject constructor(
    private val isNodeInRubbish: IsNodeInRubbish,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation function
     *
     * @param nodeId The [NodeId] of the deleted Node
     * @return true if the Node is in the Rubbish Bin and the deleted Node's path starts with the
     * substring of the SyncDebris, and false if otherwise
     */
    suspend operator fun invoke(nodeId: NodeId): Boolean {
        if (isNodeInRubbish(nodeId.longValue)) {
            val nodePath = nodeRepository.getNodePathById(nodeId)
            val nodePathLength = nodePath.length

            return when {
                nodePathLength < SYNC_DEBRIS_PATH_LENGTH -> false
                nodePathLength == SYNC_DEBRIS_PATH_LENGTH -> {
                    nodePath == SYNC_DEBRIS_PATH
                }
                else -> {
                    nodePath.subSequence(0, SYNC_DEBRIS_PATH_LENGTH + 1) == "$SYNC_DEBRIS_PATH/"
                }
            }
        } else {
            return false
        }
    }

    companion object {
        private const val SYNC_DEBRIS_PATH = "//bin/SyncDebris"
        private const val SYNC_DEBRIS_PATH_LENGTH = SYNC_DEBRIS_PATH.length
    }
}