package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.gateway.api.MegaApiGateway
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
    private val megaApiGateway: MegaApiGateway,
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

    private fun isMediaSyncFolder(nodeId: NodeId, data: FolderTypeData) =
        nodeId.longValue in listOfNotNull(
            data.primarySyncHandle,
            data.secondarySyncHandle
        )

    private fun isChatFolder(nodeId: NodeId, data: FolderTypeData) =
        data.chatFilesFolderId == nodeId

    private fun isRootBackup(nodeId: NodeId, data: FolderTypeData) = data.backupFolderId == nodeId

    private suspend fun isChildBackup(
        nodeId: NodeId,
        data: FolderTypeData,
    ) = data.backupFolderPath?.let { backupFolderPath ->
        val nodePath = megaApiGateway.getNodePathByHandle(nodeId.longValue) ?: return false
        nodePath.startsWith(backupFolderPath)
    } ?: false

    private fun isDeviceFolder(nodeId: FolderNode) = !nodeId.device.isNullOrEmpty()
}