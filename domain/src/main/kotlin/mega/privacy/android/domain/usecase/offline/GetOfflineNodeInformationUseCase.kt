package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.node.GetNestedParentFoldersUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.joinAsPath
import javax.inject.Inject

/**
 * Get the OfflineNodeInformation for this node, the file and nested folders may or may not exist and may or may not be up to date
 * Some example paths returned:
 *  - file in drive root:       /data/user/0/mega.privacy.android.app/files/MEGA Offline/Welcome.pdf
 *  - sub-folder in drive:      /data/user/0/mega.privacy.android.app/files/MEGA Offline/folder1/folder2
 *  - file in backup:           /data/user/0/mega.privacy.android.app/files/MEGA Offline/in/Backups/MacBook Pro/My backup/Screenshot.png
 *  - file in incoming shared:  /data/user/0/mega.privacy.android.app/files/MEGA Offline/229921431902040/shared with friends/notes/hola.txt
 *  - file in chats folder:     /data/user/0/mega.privacy.android.app/files/MEGA Offline/My chat files/picture.png

 * @see [IsAvailableOfflineUseCase] to know if the node is available offline or not
 */
class GetOfflineNodeInformationUseCase @Inject constructor(
    private val getNestedParentFoldersUseCase: GetNestedParentFoldersUseCase,
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke(node: Node): OfflineNodeInformation {
        var parents = getNestedParentFoldersUseCase(node)
        val isIncomingShareId = (
                node.takeIf { it.isIncomingShare }
                    ?: parents.firstOrNull()?.takeIf { it.isIncomingShare }
                )?.id
        val isInBackups = isNodeInBackupsUseCase(node.id.longValue)
        val isInCloudDrive = isNodeInCloudDriveUseCase(node.id.longValue)
        if (parents.isNotEmpty() && (isInCloudDrive || isInBackups)) {
            //we don't need the root backup parent (Vault) or root drive parent
            parents = parents.drop(1)
        }
        val path = parents.joinAsPath()

        return when {
            isInBackups -> {
                BackupsOfflineNodeInformation(
                    path = path,
                    name = node.name,
                    handle = node.id.longValue.toString(),
                    isFolder = node is FolderNode,
                )
            }

            isIncomingShareId != null -> {
                IncomingShareOfflineNodeInformation(
                    path = path,
                    name = node.name,
                    handle = node.id.longValue.toString(),
                    isFolder = node is FolderNode,
                    incomingHandle = isIncomingShareId.longValue.toString()
                )
            }

            else ->
                OtherOfflineNodeInformation(
                    path = path,
                    name = node.name,
                    handle = node.id.longValue.toString(),
                    isFolder = node is FolderNode,
                )
        }
    }
}