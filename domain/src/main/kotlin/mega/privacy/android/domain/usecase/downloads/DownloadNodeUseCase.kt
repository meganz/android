package mega.privacy.android.domain.usecase.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Downloads a node to the specified path
 */
class DownloadNodeUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Downloads a node to the specified path and returns a Flow to monitor the progress
     * @param nodeId The desired node
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [Transfer]s to monitor the download state and progress
     */
    operator fun invoke(
        nodeId: NodeId,
        destinationPath: String,
        appData: String?,
        isHighPriority: Boolean,
    ): Flow<Transfer> = flow {
        if (destinationPath.isNotEmpty()) {
            val destinationDirectory = if (nodeRepository.getNodeById(nodeId) is FolderNode) {
                destinationPath
            } else {
                fileSystemRepository.getParent(destinationPath)
            }
            fileSystemRepository.createDirectory(destinationDirectory)

            emitAll(
                transferRepository.startDownload(
                    nodeId = nodeId,
                    localPath = destinationPath,
                    appData = appData,
                    shouldStartFirst = isHighPriority
                )
            )
        }
    }
}