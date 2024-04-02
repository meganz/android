package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.DestinationUriAndSubFolders
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.HandleAvailableOfflineEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.sd.GetTransferDestinationUriUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase
import javax.inject.Inject

/**
 * Add (or update if already exists) an active transfer to local storage based on a TransferEvent
 *
 * @property transferRepository
 */
class HandleTransferEventUseCase @Inject internal constructor(
    private val transferRepository: TransferRepository,
    private val broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase,
    private val broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
    private val handleAvailableOfflineEventUseCase: HandleAvailableOfflineEventUseCase,
    private val handleSDCardEventUseCase: HandleSDCardEventUseCase,
    private val getTransferDestinationUriUseCase: GetTransferDestinationUriUseCase,
) {

    /**
     * Invoke.
     * @param event the [TransferEvent] that has been received.
     */
    suspend operator fun invoke(event: TransferEvent) {
        if (event.transfer.isVoiceClip() || event.transfer.isBackgroundTransfer() || event.transfer.isStreamingTransfer) {
            return
        }
        val transferDestination: DestinationUriAndSubFolders? =
            if (event is TransferEvent.TransferStartEvent || event is TransferEvent.TransferFinishEvent) {
                getTransferDestinationUriUseCase(event.transfer)
            } else null
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
                    transferRepository.addCompletedTransfer(
                        event.transfer,
                        event.error,
                        transferDestination?.toString()
                    )
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
        handleAvailableOfflineEventUseCase(event)
        handleSDCardEventUseCase(event, transferDestination)
    }
}