package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Default [GetFolderType] implementation
 */
class DefaultGetFolderType @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val chatRepository: ChatRepository,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val hasAncestor: HasAncestor,
    private val getDeviceType: GetDeviceType,
) : GetFolderType {
    override suspend fun invoke(folder: FolderNode) = with(folder) {
        when {
            isMediaSyncFolder(id) -> FolderType.MediaSyncFolder
            isChatFolder(id) -> FolderType.ChatFilesFolder
            isRootBackup(id) -> FolderType.RootBackup
            isChildBackup(id) -> FolderType.ChildBackup
            isDeviceFolder(this) -> FolderType.DeviceBackup(getDeviceType(this))
            else -> FolderType.Default
        }
    }


    private suspend fun isMediaSyncFolder(folder: NodeId) =
        folder.longValue in listOf(cameraUploadRepository.getPrimarySyncHandle(),
            cameraUploadRepository.getSecondarySyncHandle())

    private suspend fun isChatFolder(folder: NodeId) =
        chatRepository.getChatFilesFolderId() == folder

    private suspend fun isRootBackup(folder: NodeId) =
        monitorBackupFolder().firstOrNull()?.getOrNull() == folder

    private suspend fun isChildBackup(
        nodeId: NodeId,
    ) = monitorBackupFolder()
        .firstOrNull()
        ?.getOrNull()
        ?.let { hasAncestor(nodeId, it) } ?: false

    private fun isDeviceFolder(folder: FolderNode) =
        !folder.device.isNullOrEmpty()
}
