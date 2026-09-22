package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.GetNodeByFingerprintAndParentNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject

/**
 * Checks whether a collided node is being copied into the folder it already lives in, in which
 * case the collision can only be resolved by creating a renamed duplicate.
 */
class IsCopyToOriginUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getNodeByFingerprintAndParentNodeUseCase: GetNodeByFingerprintAndParentNodeUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param collision the [NodeNameCollision] to check
     * @return true if the collided node already lives in the target folder, false otherwise
     */
    suspend operator fun invoke(collision: NodeNameCollision): Boolean = when (collision) {
        is NodeNameCollision.Chat -> false
        is NodeNameCollision.Default -> collision.type == NodeNameCollisionType.COPY
                && isNodeInParent(collision)

        is NodeNameCollision.FileLink -> isFileLinkInParent(collision)
    }

    private suspend fun isNodeInParent(collision: NodeNameCollision.Default) = runCatching {
        getNodeByHandleUseCase(collision.nodeHandle, attemptFromFolderApi = true)
    }.getOrNull()?.let { it.parentId.longValue == collision.parentHandle } ?: false

    /**
     * A public node is not in the account tree, so the only way to recognise it is a node with the
     * same fingerprint already sitting in the target folder.
     */
    private suspend fun isFileLinkInParent(collision: NodeNameCollision.FileLink): Boolean {
        val fingerprint = (nodeRepository.getNodeFromSerializedData(collision.serializedData)
                as? FileNode)?.fingerprint ?: return false
        return getNodeByFingerprintAndParentNodeUseCase(
            fingerprint = fingerprint,
            parentNodeId = NodeId(collision.parentHandle),
        )?.let { it.parentId.longValue == collision.parentHandle } ?: false
    }
}
