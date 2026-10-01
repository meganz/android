package mega.privacy.android.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case that fetches all the data required for folder type determination during mapping
 */
class GetFolderTypeDataUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val chatRepository: ChatRepository,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Fetches all the data required for folder type determination.
     *
     * @return FolderTypeData containing all necessary information for folder type mapping
     */
    suspend operator fun invoke(): FolderTypeData = coroutineScope {
        val primarySyncHandle = async { cameraUploadsRepository.getPrimarySyncHandle() }
        val secondarySyncHandle = async { cameraUploadsRepository.getSecondarySyncHandle() }
        val chatFilesFolderId = async { chatRepository.getChatFilesFolderId() }
        val syncedNodeIds = async { nodeRepository.getAllSyncedNodeIds() }
        val backupFolderDeferred = async {
            val id = monitorBackupFolder().firstOrNull()?.getOrNull()
            val path = id?.let { nodeRepository.getNodePathById(it) }?.takeIf { it.isNotEmpty() }
            id to path
        }
        val (backupFolderId, backupFolderPath) = backupFolderDeferred.await()

        FolderTypeData(
            primarySyncHandle = primarySyncHandle.await(),
            secondarySyncHandle = secondarySyncHandle.await(),
            chatFilesFolderId = chatFilesFolderId.await(),
            backupFolderId = backupFolderId,
            backupFolderPath = backupFolderPath,
            syncedNodeIds = syncedNodeIds.await()
        )
    }
}
