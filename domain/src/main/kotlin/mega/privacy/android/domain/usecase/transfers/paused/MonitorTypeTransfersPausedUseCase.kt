package mega.privacy.android.domain.usecase.transfers.paused

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.TransferRepository

/**
 * Helper class for monitoring paused transfers by type (Downloads, general uploads, camera uploads, chat uploads)
 */
abstract class MonitorTypeTransfersPausedUseCase {
    /**
     * [TransferRepository] to query different states and counters
     */
    protected abstract val transferRepository: TransferRepository

    /**
     * @return whether this [transfer] is of the required type or not
     */
    abstract fun isCorrectType(transfer: Transfer): Boolean

    /**
     * @return the total pending individual transfers of this type
     */
    abstract suspend fun totalPendingIndividualTransfers(): Int

    /**
     * @return the total paused individual transfers of this type
     */
    abstract suspend fun totalPausedIndividualTransfers(): Int

    private suspend fun areAllIndividualTransfersPaused(): Boolean {
        val pending = totalPendingIndividualTransfers()
        return pending > 0 && totalPausedIndividualTransfers() >= pending
    }

    /**
     * Invoke the use case
     */
    operator fun invoke(): Flow<Boolean> {
        return combine(
            transferRepository.monitorPausedTransfers(),
            monitorAllIndividualTransfersPaused(),
        ) { globalPause, allIndividualPaused ->
            globalPause || allIndividualPaused
        }
    }

    private fun monitorAllIndividualTransfersPaused() =
        transferRepository.monitorTransferEvents()
            .filter {
                (it is TransferEvent.TransferPaused || it is TransferEvent.TransferFinishEvent || it is TransferEvent.TransferStartEvent)
                        && !it.transfer.isBackgroundTransfer()
                        && !it.transfer.isVoiceClip()
                        && !it.transfer.isFolderTransfer
                        && isCorrectType(it.transfer)
            }
            .map {
                //if it's a resume event it means at least this one is not paused, if not we need to check
                (it as? TransferEvent.TransferPaused)?.paused != false && areAllIndividualTransfersPaused()
            }.onStart {
                emit(areAllIndividualTransfersPaused())
            }
}