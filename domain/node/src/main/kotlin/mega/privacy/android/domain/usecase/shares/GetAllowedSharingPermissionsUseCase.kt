package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
import javax.inject.Inject

/**
 * Use case to get the allowed sharing permissions for a node
 *
 * @property checkBackupNodeTypeUseCase Use case to check the backup node type
 * @property getNodeByIdUseCase Use case to get a node by its ID
 */
class GetAllowedSharingPermissionsUseCase @Inject constructor(
    private val checkBackupNodeTypeUseCase: CheckBackupNodeTypeUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {
    suspend operator fun invoke(nodeId: NodeId): Set<AccessPermission> {
        val node = getNodeByIdUseCase(nodeId) ?: throw NodeDoesNotExistsException()
        return when (checkBackupNodeTypeUseCase(node)) {
            BackupNodeType.NonBackupNode -> AccessPermission.entries.toSet()
            else -> setOf(AccessPermission.READ)
        }
    }
}