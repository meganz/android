package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

/**
 * Use case that returns a flow that emits when all ongoing active transfers have been cancelled
 */
class MonitorLastTransfersHaveBeenCancelledUseCase @Inject constructor(
    private val monitorTransfersStatusUseCase: MonitorTransfersStatusUseCase,
) {

    /**
     * Invoke
     */
    operator fun invoke(): Flow<Unit> =
        monitorTransfersStatusUseCase().scan(
            PendingAndCancelledAccumulator()
        ) { previous, transfersStatusInfo ->

            PendingAndCancelledAccumulator(
                currentPending = transfersStatusInfo.pendingUploads + transfersStatusInfo.pendingDownloads,
                currentCancelled = transfersStatusInfo.cancelled,
                previousPending = previous.currentPending,
                previousCancelled = previous.currentCancelled,
            )
        }.filter { it.previousPendingHaveBeenCancelled() }.map { }

    private data class PendingAndCancelledAccumulator(
        val currentPending: Int = 0,
        val currentCancelled: Int = 0,
        val previousPending: Int = 0,
        val previousCancelled: Int = 0,
    ) {
        fun previousPendingHaveBeenCancelled() =
            currentPending == 0 && previousPending != 0 && previousPending == currentCancelled - previousCancelled
    }
}