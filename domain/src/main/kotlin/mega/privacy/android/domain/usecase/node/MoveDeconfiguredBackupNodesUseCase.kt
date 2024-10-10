package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to Move the nodes that used to be part of backup.
 */
class MoveDeconfiguredBackupNodesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Move the nodes that used to be part of backup.
     *
     * @param deconfiguredBackupRoot The [NodeId] of the Backup to move
     * @param backupDestination The [NodeId] that [deconfiguredBackupRoot] will be moved to
     */
    suspend operator fun invoke(
        deconfiguredBackupRoot: NodeId,
        backupDestination: NodeId,
    ) = nodeRepository.moveOrRemoveDeconfiguredBackupNodes(
        deconfiguredBackupRoot = deconfiguredBackupRoot,
        backupDestination = backupDestination,
    )
}