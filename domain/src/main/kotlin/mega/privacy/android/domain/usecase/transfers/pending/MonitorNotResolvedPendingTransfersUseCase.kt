package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Use case to monitor the pending transfers that are not yet resolved, so they still need to be started or the SDK is still scanning them
 *
 * Once all the transfers are started and scanned by the SDK it will wait a maximum of 400 milliseconds for first update events,
 * to try to give a better update of transfers actually started or cancelled because they already exists.
 * In that way the message shown to the user can be more accurate but without blocking it too much
 */
class MonitorNotResolvedPendingTransfersUseCase @Inject constructor(
    private val getPendingTransfersByTypeUseCase: GetPendingTransfersByTypeUseCase,
) {
    /**
     * Invoke
     */
    operator fun invoke(transferType: TransferType): Flow<List<PendingTransfer>> {
        var waitJob: Job? = null

        return channelFlow {
            getPendingTransfersByTypeUseCase(transferType)
                .transformWhile { pendingTransfers ->
                    val notResolved = pendingTransfers.filter { it.notResolved() }
                    emit(notResolved)
                    if (waitJob == null && isWaitingForAlreadyStarted(pendingTransfers)) {
                        // If all nodes are scanned, wait a bit for a better UX message when there are already downloaded/uploaded files. Close it if it takes long.
                        waitJob = launch {
                            delay(400.milliseconds)
                            channel.close()
                        }
                    }
                    notResolved.any().also {
                        if (!it) waitJob?.cancel()
                    }
                }.collect {
                    send(it)
                }
        }
    }

    private fun isWaitingForAlreadyStarted(pendingTransfers: List<PendingTransfer>) =
        pendingTransfers.any { it.state == PendingTransferState.SdkScanned } && pendingTransfers.all {
            it.state in listOf(
                PendingTransferState.SdkScanned,
                PendingTransferState.AlreadyStarted,
                PendingTransferState.ErrorStarting,
            )
        }
}