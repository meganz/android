package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to Remove the nodes that used to be part of backup.
 */
class RemoveDeconfiguredBackupNodesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Remove the nodes that used to be part of backup.
     *
     * @param deconfiguredBackupRoot The [NodeId] of the Backup to remove
     */
    suspend operator fun invoke(
        deconfiguredBackupRoot: NodeId,
    ) = nodeRepository.moveOrRemoveDeconfiguredBackupNodes(
        deconfiguredBackupRoot = deconfiguredBackupRoot,
        backupDestination = NodeId(-1L),
    )
}