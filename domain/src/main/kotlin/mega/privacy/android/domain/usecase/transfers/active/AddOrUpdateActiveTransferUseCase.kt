package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Add (or update if already exists) an active transfer to local storage based on a TransferEvent
 *
 * @property transferRepository
 */
class AddOrUpdateActiveTransferUseCase @Inject internal constructor(
    private val transferRepository: TransferRepository,
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
                if (event.error == null || event.transfer.state == TransferState.STATE_CANCELLED) {
                    transferRepository.insertOrUpdateActiveTransfer(event.transfer)
                    transferRepository.updateTransferredBytes(event.transfer)
                } else {
                    //TRAN-228: handle the error similar to what is done in DownloadService.doOnTransferFinish
                }
            }

            is TransferEvent.TransferTemporaryErrorEvent -> {
                if (event.error is QuotaExceededMegaException && event.error.value != 0L) {
                    //TRAN-228: show TRANSFER OVERQUOTA ERROR similar to what is done in DownloadService.checkTransferOverQuota
                }
            }
        }
    }
}