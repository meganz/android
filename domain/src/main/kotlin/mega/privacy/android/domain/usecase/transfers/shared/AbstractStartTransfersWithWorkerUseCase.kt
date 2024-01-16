package mega.privacy.android.domain.usecase.transfers.shared

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shared implementation for upload and download transfers that will be monitored by a Worker
 */
abstract class AbstractStartTransfersWithWorkerUseCase(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) {
    internal suspend fun FlowCollector<MultiTransferEvent>.startTransfersAndWorker(
        doTransfers: () -> Flow<MultiTransferEvent>,
        startWorker: suspend () -> Unit,
    ) = emitAll(doTransfers()
        .filter {
            it !is MultiTransferEvent.SingleTransferEvent
        }.transformWhile { event ->
            val finished = event is MultiTransferEvent.ScanningFoldersFinished
            //emitting a FinishProcessingTransfers can cause a terminal event in the collector (firstOrNull for instance), so we need to start the worker before emitting it
            if (finished) {
                startWorker()
            }
            emit(event)
            return@transformWhile !finished
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
    )
}