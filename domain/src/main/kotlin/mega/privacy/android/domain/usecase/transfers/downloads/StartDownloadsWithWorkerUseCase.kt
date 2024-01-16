package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.LocalStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractStartTransfersWithWorkerUseCase
import java.io.File
import javax.inject.Inject


/**
 * Start downloading a list of nodes to the specified path and returns a Flow to monitor the progress until the nodes are scanned.
 * While the returned flow is not completed the app should be blocked to avoid other interaction with the sdk to avoid issues
 * Once the flow is completed the sdk will keep downloading and a DownloadWorker will monitor updates globally.
 * If cancelled before completion the processing of the nodes will be cancelled
 */
class StartDownloadsWithWorkerUseCase @Inject constructor(
    private val doesPathHaveSufficientSpaceForNodesUseCase: DoesPathHaveSufficientSpaceForNodesUseCase,
    private val downloadNodesUseCase: DownloadNodesUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val startDownloadWorkerUseCase: StartDownloadWorkerUseCase,
    private val isDownloadsWorkerStartedUseCase: IsDownloadsWorkerStartedUseCase,
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) : AbstractStartTransfersWithWorkerUseCase(
    cancelCancelTokenUseCase,
) {
    /**
     * Invoke
     * @param nodes The desired nodes to download
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [MultiTransferEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        nodes: List<TypedNode>,
        destinationPath: String,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        if (destinationPath.isEmpty()) {
            return nodes.asFlow().map { MultiTransferEvent.TransferNotStarted(it.id, null) }
        }
        //wrap the startDownloadFlow to be able to execute suspended functions
        return flow {
            val appData: TransferAppData?
            val finalDestinationPath = if (fileSystemRepository.isSDCardPath(destinationPath)) {
                appData = TransferAppData.SdCardDownload(destinationPath, null)
                fileSystemRepository.getOrCreateSDCardCacheFolder()?.path?.plus(File.separator)
                    ?: run {
                        nodes.forEach {
                            emit(
                                MultiTransferEvent.TransferNotStarted(
                                    it.id,
                                    LocalStorageException(null, null)
                                )
                            )
                        }
                        return@flow
                    }
            } else {
                appData = null
                destinationPath
            }
            fileSystemRepository.createDirectory(finalDestinationPath)
            if (!doesPathHaveSufficientSpaceForNodesUseCase(finalDestinationPath, nodes)) {
                emit(MultiTransferEvent.InsufficientSpace)
            } else {
                startTransfersAndWorker(
                    doTransfers = {
                        downloadNodesUseCase(
                            nodes,
                            finalDestinationPath,
                            appData = appData,
                            isHighPriority = isHighPriority
                        )
                    },
                    startWorker = {
                        startDownloadWorkerUseCase()
                        //ensure worker has started and is listening to global events so we can finish downloadNodesUseCase
                        isDownloadsWorkerStartedUseCase()
                    },
                )
            }
        }
    }
}