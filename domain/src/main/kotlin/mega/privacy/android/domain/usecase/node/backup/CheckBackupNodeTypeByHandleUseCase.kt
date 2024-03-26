package mega.privacy.android.domain.usecase.node.backup

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.repository.BackupRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import javax.inject.Inject

/**
 * Use case to check backup node type
 */
class CheckBackupNodeTypeByHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val backupRepository: BackupRepository,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
) {

    /**
     * invoke
     * @param node [Node]
     * @return [BackupNodeType]
     */
    suspend operator fun invoke(node: Node): BackupNodeType {
        val deviceId = backupRepository.getDeviceId()

        return if (nodeRepository.isNodeInBackups(node.id.longValue).not() ||
            isNodeInRubbishBinUseCase(node.id)
        ) {
            BackupNodeType.NonBackupNode
        } else if (nodeRepository.getBackupsNode()?.id?.longValue == node.id.longValue) {
            BackupNodeType.RootNode
        } else if (nodeRepository.getBackupsNode()?.id?.longValue == node.parentId.longValue &&
            deviceId.isNullOrBlank().not()
        ) {
            BackupNodeType.DeviceNode
        } else if (node.parentId.longValue == nodeRepository.getNodeById(node.parentId)?.id?.longValue &&
            deviceId.isNullOrBlank().not()
        ) {
            BackupNodeType.FolderNode
        } else {
            BackupNodeType.ChildFolderNode
        }
    }
}
