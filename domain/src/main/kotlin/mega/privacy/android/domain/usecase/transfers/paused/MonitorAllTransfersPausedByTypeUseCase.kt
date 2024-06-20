package mega.privacy.android.domain.usecase.transfers.paused

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for monitoring paused transfers by type (Downloads, general uploads, camera uploads, chat uploads)
 * [TransferRepository] to query different states and counters
 */
class MonitorAllTransfersPausedByTypeUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke the use case
     */
    operator fun invoke(type: TransferType): Flow<Boolean> {
        return combine(
            transferRepository.monitorPausedTransfers(),
            monitorAllIndividualTransfersPaused(type),
        ) { globalPause, allIndividualPaused ->
            globalPause || allIndividualPaused
        }
    }

    private fun monitorAllIndividualTransfersPaused(type: TransferType) = when (type) {
        TransferType.NONE -> flowOf(false)
        TransferType.CU_UPLOAD -> {
            transferRepository.monitorTransferEvents()
                .filter {
                    (it is TransferEvent.TransferPaused || it is TransferEvent.TransferFinishEvent || it is TransferEvent.TransferStartEvent)
                            && !it.transfer.isBackgroundTransfer()
                            && !it.transfer.isVoiceClip()
                            && !it.transfer.isFolderTransfer
                            && it.transfer.transferType == type
                }
                .map {
                    //if it's a resume event it means at least this one is not paused, if not we need to check
                    (it as? TransferEvent.TransferPaused)?.paused != false
                            && areAllIndividualCameraUploadsTransfersPaused()
                }.onStart {
                    emit(areAllIndividualCameraUploadsTransfersPaused())
                }
        }

        else -> {
            transferRepository.getActiveTransferTotalsByType(type).map { it.allPaused() }
        }
    }

    private suspend fun areAllIndividualCameraUploadsTransfersPaused(): Boolean {
        val pending = transferRepository.getNumPendingCameraUploads()
        return pending > 0 && transferRepository.getNumPendingPausedCameraUploads() >= pending
    }
}