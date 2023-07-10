package mega.privacy.android.domain.usecase.transfer

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.downloads.DownloadNodesUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException


/**
 * Start downloading a list of nodes to the specified path and returns a Flow to monitor the progress until the nodes are processed.
 * While the returned flow is not completed the app should be blocked to avoid other interaction with the sdk to avoid issues
 * Once the flow is completed the sdk will keep downloading and a DownloadWorker will monitor updates globally.
 * If cancelled before completion the processing of the nodes will be cancelled
 */
class StartDownloadUseCase @Inject constructor(
    private val doesPathHaveSufficientSpaceForNodesUseCase: DoesPathHaveSufficientSpaceForNodesUseCase,
    private val downloadNodesUseCase: DownloadNodesUseCase,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param nodes The desired nodes to download
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [DownloadNodesEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        nodes: List<Node>,
        destinationPath: String,
        appData: TransferAppData?,
        isHighPriority: Boolean,
    ): Flow<DownloadNodesEvent> {
        if (destinationPath.isEmpty()) {
            return nodes.asFlow().map { DownloadNodesEvent.TransferNotStarted(it.id, null) }
        }
        //wrap the startDownloadFlow to be able to create the directory since it's a suspending function
        return flow {
            fileSystemRepository.createDirectory(destinationPath)
            val startDownloadFlow =
                if (doesPathHaveSufficientSpaceForNodesUseCase(destinationPath, nodes)) {
                    downloadNodesUseCase(
                        nodes.map { it.id },
                        destinationPath,
                        appData = appData,
                        isHighPriority = isHighPriority
                    ).filter {
                        it !is DownloadNodesEvent.SingleTransferEvent
                    }.transformWhile { event ->
                        emit(event)
                        // complete the flow on finish processing
                        event !is DownloadNodesEvent.FinishProcessingTransfers
                    }
                        .cancellable()
                        .onCompletion { error ->
                            if (error != null) {
                                //if the start download is canceled before finishing processing we need to cancel the processing operation
                                withContext(NonCancellable) {
                                    cancelCancelTokenUseCase()
                                }
                                if (error !is CancellationException) {
                                    throw error
                                }
                            } else {
                                //to be done in TRAN-169: here the download worker will be started to monitor the download and show the notification
                            }
                        }
                } else {
                    flowOf(DownloadNodesEvent.NotSufficientSpace)
                }
            emitAll(startDownloadFlow)
        }
    }
}