package mega.privacy.android.domain.usecase.transfers.downloads

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
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.LocalStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import java.io.File
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
    private val startDownloadWorkerUseCase: StartDownloadWorkerUseCase,
    private val ensureDownloadsWorkerHasStartedUseCase: EnsureDownloadsWorkerHasStartedUseCase,
) {
    /**
     * Invoke
     * @param nodes The desired nodes to download
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [DownloadNodesEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        nodes: List<TypedNode>,
        destinationPath: String,
        isHighPriority: Boolean,
    ): Flow<DownloadNodesEvent> {
        if (destinationPath.isEmpty()) {
            return nodes.asFlow().map { DownloadNodesEvent.TransferNotStarted(it.id, null) }
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
                                DownloadNodesEvent.TransferNotStarted(
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
            val startDownloadFlow =
                if (doesPathHaveSufficientSpaceForNodesUseCase(finalDestinationPath, nodes)) {
                    downloadNodesUseCase(
                        nodes,
                        finalDestinationPath,
                        appData = appData,
                        isHighPriority = isHighPriority
                    ).filter {
                        it !is DownloadNodesEvent.SingleTransferEvent
                    }.transformWhile { event ->
                        val finished = event is DownloadNodesEvent.FinishProcessingTransfers
                        //emitting a FinishProcessingTransfers can cause a terminal event in the collector (firstOrNull for instance), so we need to start the worker before emitting it
                        if (finished) {
                            startDownloadWorkerUseCase()

                            //ensure worker has started and is listening to global events so we can finish downloadNodesUseCase
                            ensureDownloadsWorkerHasStartedUseCase()
                        }
                        emit(event)
                        return@transformWhile !finished
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
                            }
                        }
                } else {
                    flowOf(DownloadNodesEvent.NotSufficientSpace)
                }
            emitAll(startDownloadFlow)
        }
    }
}