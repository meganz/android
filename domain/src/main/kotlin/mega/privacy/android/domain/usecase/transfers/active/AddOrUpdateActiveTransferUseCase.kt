package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import javax.inject.Inject

/**
 * Add (or update if already exists) an active transfer to local storage based on a TransferEvent
 *
 * @property transferRepository
 */
class AddOrUpdateActiveTransferUseCase @Inject internal constructor(
    private val transferRepository: TransferRepository,
    private val broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase,
    private val broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
) {

    /**
     * Invoke.
     * @param event the [TransferEvent] that has been received.
     */
    suspend operator fun invoke(event: TransferEvent) {
        if (event.transfer.isVoiceClip() || event.transfer.isBackgroundTransfer()) {
            return
        }

        when (event) {
            is TransferEvent.TransferStartEvent, is TransferEvent.TransferPaused -> {
                transferRepository.insertOrUpdateActiveTransfer(event.transfer)
            }

            is TransferEvent.TransferUpdateEvent -> {
                transferRepository.updateTransferredBytes(event.transfer)
            }

            is TransferEvent.TransferDataEvent -> {
                //we don't need to handle this event here
            }

            is TransferEvent.TransferFinishEvent -> {
                transferRepository.insertOrUpdateActiveTransfer(event.transfer)
                transferRepository.updateTransferredBytes(event.transfer)
                if (!event.transfer.isFolderTransfer) {
                    transferRepository.addCompletedTransfer(event.transfer, event.error)
                }

                if (event.error is BusinessAccountExpiredMegaException) {
                    broadcastBusinessAccountExpiredUseCase()
                }
            }

            is TransferEvent.TransferTemporaryErrorEvent -> {
                if (event.error is QuotaExceededMegaException && event.error.value != 0L) {
                    broadcastTransferOverQuotaUseCase(true)
                }
            }
        }
    }
}