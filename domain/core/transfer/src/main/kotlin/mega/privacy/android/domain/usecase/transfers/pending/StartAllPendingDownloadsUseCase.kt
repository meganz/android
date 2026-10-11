package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.DownloadBatchItem
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isTransferUpdated
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferQuotaStateUseCase
import javax.inject.Inject

/**
 * Use case to start all pending transfers of download type.
 *
 * Every root of one download action goes to the SDK as a single batch, so it can predict the
 * transfer quota state for the action as a whole and the user can be warned before the bandwidth
 * is spent.
 */
class StartAllPendingDownloadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    getPendingTransfersByTypeAndStateUseCase: GetPendingTransfersByTypeAndStateUseCase,
    updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    updatePendingTransferStartedCountUseCase: UpdatePendingTransferStartedCountUseCase,
    private val getTypedNodeFromPendingTransferUseCase: GetTypedNodeFromPendingTransferUseCase,
    private val broadcastTransferQuotaStateUseCase: BroadcastTransferQuotaStateUseCase,
) : StartAllPendingTransfersUseCase(
    TransferType.DOWNLOAD,
    transferRepository,
    getPendingTransfersByTypeAndStateUseCase,
    updatePendingTransferStateUseCase,
    updatePendingTransferStartedCountUseCase
) {
    override suspend fun doTransfers(
        pendingTransfers: List<PendingTransfer>,
        scope: CoroutineScope,
    ): Map<PendingTransfer, Flow<TransferEvent>> = pendingTransfers
        // One batch per download action, so the quota prediction covers the whole action.
        .groupBy { pendingTransfer ->
            pendingTransfer.appData
                ?.filterIsInstance<TransferAppData.TransferGroup>()
                ?.firstOrNull()?.groupId
        }
        .flatMap { (_, group) -> startBatch(group, scope).entries }
        .associate { it.key to it.value }

    private suspend fun startBatch(
        group: List<PendingTransfer>,
        scope: CoroutineScope,
    ): Map<PendingTransfer, Flow<TransferEvent>> {
        val resolved = group.mapNotNull { pendingTransfer ->
            resolveNode(pendingTransfer)?.let { pendingTransfer to it }
        }
        if (resolved.isEmpty()) return emptyMap()

        val channels = resolved.map { (pendingTransfer, _) ->
            pendingTransfer to Channel<TransferEvent>(Channel.UNLIMITED)
        }
        val items = resolved.map { (pendingTransfer, node) ->
            DownloadBatchItem(
                node = node,
                destinationPath = pendingTransfer.uriPath.value,
                fileName = pendingTransfer.fileName,
                appData = pendingTransfer.appData,
                isHighPriority = pendingTransfer.isHighPriority,
            )
        }

        scope.launch {
            try {
                transferRepository.startDownloadBatch(items).collect { event ->
                    when {
                        event is TransferEvent.TransferQuotaStateEvent -> {
                            Log.d("$LOG_TAG predicted transfer quota state ${event.state}")
                            broadcastTransferQuotaStateUseCase(event.state)
                        }

                        // The batch reports these for the whole download and a pending transfer
                        // does nothing with them, so forwarding would pile up in channels the
                        // flows stopped reading once they resolved.
                        event is TransferEvent.TransferUpdateEvent
                                && !event.isTransferUpdated -> Unit

                        else -> {
                            // The batch is the transfer of every item in it: the SDK reports the
                            // individual files to the global listener, not here.
                            channels.forEach { (_, channel) -> channel.trySend(event) }
                            if (event is TransferEvent.TransferFinishEvent) {
                                channels.forEach { (_, channel) -> channel.close() }
                            }
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                // None of the item flows carries a batch level failure, so they are failed here.
                Log.e("$LOG_TAG batch of ${items.size} failed to start", throwable)
                resolved.forEach { (pendingTransfer, _) ->
                    errorOnStartingPendingTransfer(pendingTransfer, throwable)
                }
            } finally {
                channels.forEach { (_, channel) -> channel.close() }
            }
        }

        return channels.associate { (pendingTransfer, channel) ->
            pendingTransfer to channel.receiveAsFlow()
        }
    }

    private suspend fun resolveNode(pendingTransfer: PendingTransfer): TypedNode? {
        val node = runCatching {
            getTypedNodeFromPendingTransferUseCase(pendingTransfer)
        }.getOrElse {
            errorOnStartingPendingTransfer(pendingTransfer, it)
            return null
        }
        if (node == null) {
            errorOnStartingPendingTransfer(pendingTransfer, NodeDoesNotExistsException())
        }
        return node
    }

    private companion object {
        const val LOG_TAG = "[batch-download]"
    }
}
