package mega.privacy.android.domain.usecase.transfers.shared

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shared implementation for upload and download transfers that will be monitored by a Worker
 */
abstract class AbstractStartTransfersWithWorkerUseCase(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) {

    /**
     * Starts the transfers and when all nodes are scanned by SDK, starts the worker.
     * If the flow is canceled before finishing processing the nodes, the transfers will
     * be canceled with [cancelCancelTokenUseCase].
     * The flow will wait until the worker is started and then will wait until all
     * the file nodes are updated or a timeout. This will allow to have updated data to show
     * the "Start Download" message to the user, with information about the number of files
     * already downloaded, etc. but with a timeout to avoid showing it to far from the context.
     */
    internal fun startTransfersAndThenWorkerFlow(
        doTransfers: suspend () -> Flow<MultiTransferEvent>,
        startWorker: suspend () -> Unit,
    ) = channelFlow {
        var workerTriggered = false
        var workerStarted = false
        doTransfers()
            .filter {
                (it as? MultiTransferEvent.SingleTransferEvent)?.transferEvent is TransferEvent.FolderTransferUpdateEvent
                        || it !is MultiTransferEvent.SingleTransferEvent
                        || (it.scanningFinished || it.allTransfersUpdated)
            }.transformWhile { event ->
                val singleTransferEvent = event as? MultiTransferEvent.SingleTransferEvent
                emit(event)
                if (!workerTriggered && singleTransferEvent?.scanningFinished == true) {
                    workerTriggered = true
                    launch {
                        withContext(NonCancellable) {
                            startWorker()
                            workerStarted = true
                            // Once the Worker has started it will wait for [allNodesUpdated] to be true but with this timeout
                            delay(800.milliseconds)
                            channel.close()
                        }
                    }
                }

                return@transformWhile !workerStarted || singleTransferEvent?.allTransfersUpdated != true
            }.onCompletion {
                if (it == null && !workerTriggered) {
                    startWorker()
                }
            }.collect {
                send(it)
            }
    }
        .cancellable()
        .onCompletion { error ->
            if (error != null) {
                //if the doTransfer is canceled before finishing processing we need to cancel the processing operation
                withContext(NonCancellable) {
                    cancelCancelTokenUseCase()
                }
                if (error !is CancellationException) {
                    throw error
                }
            }
        }
}