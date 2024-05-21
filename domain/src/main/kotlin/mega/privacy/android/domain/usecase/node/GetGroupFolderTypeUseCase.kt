package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import javax.inject.Inject

/**
 * Get group folder type use case
 *
 */
class GetGroupFolderTypeUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val chatRepository: ChatRepository,
    private val monitorBackupFolder: MonitorBackupFolder,
) {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): Map<NodeId, FolderType> = mapOf(
        cameraUploadsRepository.getPrimarySyncHandle()
            ?.let { NodeId(it) } to FolderType.MediaSyncFolder,
        cameraUploadsRepository.getSecondarySyncHandle()?.let {
            NodeId(it)
        } to FolderType.MediaSyncFolder,
        chatRepository.getChatFilesFolderId() to FolderType.ChatFilesFolder,
        monitorBackupFolder().firstOrNull()?.getOrNull() to FolderType.RootBackup
    ).filterKeys { it != null }.mapKeys { it.key!! }
}