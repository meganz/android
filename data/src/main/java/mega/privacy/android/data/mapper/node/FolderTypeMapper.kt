package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetDeviceType
import javax.inject.Inject

/**
 * Mapper that determines folder types based on folder node and pre-fetched data.
 */
class FolderTypeMapper @Inject constructor(
    private val getDeviceType: GetDeviceType,
    // private val hasAncestor: HasAncestor,
) {

    /**
     * Determines the folder type for a given folder using pre-fetched data.
     *
     * @param folder The folder to analyze
     * @param data Pre-fetched data containing all required information
     * @return The determined folder type
     */
    suspend operator fun invoke(folder: FolderNode, data: FolderTypeData): FolderType =
        with(folder) {
            when {
                isMediaSyncFolder(id, data) -> FolderType.MediaSyncFolder
                isChatFolder(id, data) -> FolderType.ChatFilesFolder
                isRootBackup(id, data) -> FolderType.RootBackup
                isChildBackup(id, data) -> FolderType.ChildBackup
                isDeviceFolder(this) -> FolderType.DeviceBackup(getDeviceType(this))
                folder.isSynced -> FolderType.Sync
                else -> FolderType.Default
            }
        }

    private fun isMediaSyncFolder(folder: NodeId, data: FolderTypeData) =
        folder.longValue in listOfNotNull(
            data.primarySyncHandle,
            data.secondarySyncHandle
        )

    private fun isChatFolder(folder: NodeId, data: FolderTypeData) =
        data.chatFilesFolderId == folder

    private fun isRootBackup(folder: NodeId, data: FolderTypeData) =
        data.backupFolderId == folder

    private fun isChildBackup(nodeId: NodeId, data: FolderTypeData) =
        data.backupFolderId?.let { backupFolderId ->
            // hasAncestor(nodeId, backupFolderId) // TODO
            false
        } ?: false

    private fun isDeviceFolder(folder: FolderNode) =
        !folder.device.isNullOrEmpty()
}