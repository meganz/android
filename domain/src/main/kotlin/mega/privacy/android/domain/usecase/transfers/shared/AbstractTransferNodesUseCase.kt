package mega.privacy.android.domain.usecase.transfers.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase

/**
 * Helper class to implement common logic for transfer multiple items (upload or download)
 * @param T type of the items to be transferred
 * @param R type of the items key to match the item with the related transfer
 */
abstract class AbstractTransferNodesUseCase<T, R>(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    private val addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    private val handleSDCardEventUseCase: HandleSDCardEventUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
) {

    internal abstract fun generateIdFromItem(item: T): R
    internal abstract fun generateIdFromTransferEvent(transferEvent: TransferEvent): R

    internal fun commonInvoke(
        items: List<T>,
        beforeStartTransfer: (suspend () -> Unit)?,
        doTransfer: (T) -> Flow<TransferEvent>,
    ): Flow<MultiTransferEvent> {
        val alreadyScanned = mutableSetOf<R>()
        val allIds = items.map(::generateIdFromItem)
        var scanningFinishedSend = false
        return channelFlow {
            monitorTransferEvents()
            //start all transfers in parallel
            items.map { node ->
                launch {
                    doTransfer(node)
                        .catch { cause ->
                            val id = generateIdFromItem(node)
                            if (cause is NodeDoesNotExistsException) {
                                send(MultiTransferEvent.TransferNotStarted(id, cause))
                            }
                            alreadyScanned.add(id)
                        }
                        .collect {
                            totalBytesMap[it.transfer.tag] = it.transfer.totalBytes
                            transferredBytesMap[it.transfer.tag] = it.transfer.transferredBytes
                            send(
                                MultiTransferEvent.SingleTransferEvent(
                                    it,
                                    transferredBytes,
                                    totalBytes
                                )
                            )
                        }
                }
            }.joinAll()
            close()
        }
            .onStart {
                beforeStartTransfer?.invoke()
            }
            .buffer(capacity = Channel.UNLIMITED)
            .transform { event ->
                emit(event)

                if (event is MultiTransferEvent.SingleTransferEvent) {
                    if (event.transferEvent is TransferEvent.TransferStartEvent) {
                        rootTags += event.transferEvent.transfer.tag
                    }
                    handleSDCardEventUseCase(event.transferEvent)
                    //update active transfers db
                    addOrUpdateActiveTransferUseCase(event.transferEvent)

                    //check if is a single node scanning finish event
                    if (event.isFinishScanningEvent) {
                        val id = generateIdFromTransferEvent(event.transferEvent)
                        if (!alreadyScanned.contains(id)) {
                            //this node is already scanned: save it and emit the event
                            alreadyScanned.add(id)

                            //check if all nodes have been scanned
                            if (!scanningFinishedSend && alreadyScanned.containsAll(allIds)) {
                                scanningFinishedSend = true
                                invalidateCancelTokenUseCase() //we need to avoid a future cancellation from now on
                                emit(MultiTransferEvent.ScanningFoldersFinished)
                            }
                        }
                    }
                }
            }.onCompletion {
                runCatching { cancelCancelTokenUseCase() }
            }.cancellable()
    }

    /**
     * tags of the transfers directly initiated by a sdk call, so we can check children transfers of all nodes
     */
    private val rootTags = mutableListOf<Int>()

    /**
     * total bytes for each transfer directly initiated by a sdk call, so we can compute the sum of all nodes
     */
    private val totalBytesMap = mutableMapOf<Int, Long>()
    private val totalBytes get() = totalBytesMap.values.sum()

    /**
     * total transferredBytes for each transfer directly initiated by a sdk call, so we can compute the sum of all nodes
     */
    private val transferredBytesMap = mutableMapOf<Int, Long>()
    private val transferredBytes get() = transferredBytesMap.values.sum()

    /**
     * Monitors download child transfer global events and update the related active transfers
     */
    private fun CoroutineScope.monitorTransferEvents() =
        this.launch {
            monitorTransferEventsUseCase()
                .filter { event ->
                    //only children as events of the related nodes are already handled
                    event.transfer.folderTransferTag?.let { rootTags.contains(it) } == true
                }
                .collect { transferEvent ->
                    withContext(NonCancellable) {
                        handleSDCardEventUseCase(transferEvent)
                        addOrUpdateActiveTransferUseCase(transferEvent)
                    }
                }
        }
}