package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.HandleAvailableOfflineEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
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
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase,
    private val handleAvailableOfflineEventUseCase: HandleAvailableOfflineEventUseCase,
    private val handleSDCardEventUseCase: HandleSDCardEventUseCase,
    private val getTransferDestinationUriUseCase: GetTransferDestinationUriUseCase,
) {

    /**
     * Invoke.
     * @param events the [TransferEvent] that has been received.
     */
    suspend operator fun invoke(vararg events: TransferEvent) {
        val transferEvents = events.filterNot { event ->
            event.transfer.isVoiceClip() || event.transfer.isBackgroundTransfer()
                    || event.transfer.isStreamingTransfer
                    || event.transfer.isBackupTransfer
        }
        if (transferEvents.isEmpty()) return
        val eventsWithDestinationMap = transferEvents.associateWith { event ->
            if (event is TransferEvent.TransferStartEvent || event is TransferEvent.TransferFinishEvent) {
                getTransferDestinationUriUseCase(event.transfer)
            } else null
        }

        checkOverQuota(transferEvents)
        checkBusinessAccountExpired(transferEvents)

        transferEvents.forEach { event ->
            handleAvailableOfflineEventUseCase(event)
            handleSDCardEventUseCase(event, eventsWithDestinationMap[event])
        }

        updateInProgressTransfers(transferEvents)
        updateTransferredBytes(transferEvents)
        updateActiveTransfers(transferEvents)

        val completedEventsMap = transferEvents
            .filterNot { it.transfer.isFolderTransfer }
            .filterIsInstance<TransferEvent.TransferFinishEvent>()
            .associateWith { eventsWithDestinationMap[it]?.toString() }
        if (completedEventsMap.isNotEmpty()) {
            transferRepository.addCompletedTransfers(completedEventsMap)
        }
    }

    private suspend fun checkOverQuota(events: List<TransferEvent>) {
        // check transfer over quota
        events.scan(null as Boolean?) { previous, it ->
            when {
                !it.transfer.transferType.isDownloadType() -> previous
                it is TransferEvent.TransferStartEvent || it is TransferEvent.TransferUpdateEvent -> false
                (it as? TransferEvent.TransferTemporaryErrorEvent)?.error is QuotaExceededMegaException -> true
                else -> previous
            }
        }.lastOrNull()?.let { transferOverQuota ->
            broadcastTransferOverQuotaUseCase(transferOverQuota)
        }

        // check storage over quota
        events.scan(null as Boolean?) { previous, it ->
            when {
                !it.transfer.transferType.isUploadType() -> previous
                it is TransferEvent.TransferStartEvent || it is TransferEvent.TransferUpdateEvent -> false
                (it as? TransferEvent.TransferTemporaryErrorEvent)?.error is QuotaExceededMegaException -> true
                else -> previous
            }
        }.lastOrNull()?.let { transferOverQuota ->
            broadcastStorageOverQuotaUseCase(transferOverQuota)
        }
    }

    private suspend fun updateInProgressTransfers(events: List<TransferEvent>) {
        events.filterByType(
            start = true,
            update = true,
            pause = true,
        )?.map { it.transfer }?.let {
            transferRepository.updateInProgressTransfers(it)
        }
    }

    private suspend fun checkBusinessAccountExpired(events: List<TransferEvent>) {
        if (events.filterIsInstance<TransferEvent.TransferFinishEvent>()
                .any { it.error is BusinessAccountExpiredMegaException }
        ) {
            broadcastBusinessAccountExpiredUseCase()
        }
    }

    private suspend fun updateTransferredBytes(events: List<TransferEvent>) {
        events.filterByType(
            update = true,
            finish = true
        )?.map { it.transfer }?.forEach {
            transferRepository.updateTransferredBytes(it)
        }
    }

    private suspend fun updateActiveTransfers(events: List<TransferEvent>) {
        events.filterByType(
            start = true,
            pause = true,
            finish = true
        )?.map { it.transfer }?.let {
            transferRepository.insertOrUpdateActiveTransfers(it)
        }
    }

    /**
     * Returns a new list containing all the events of any of these types but without duplicates. In case events for the same transfer are found, only the last one is keep. If the list is empty it returns null.
     */
    private fun List<TransferEvent>.filterByType(
        start: Boolean = false,
        update: Boolean = false,
        pause: Boolean = false,
        finish: Boolean = false,
    ): List<TransferEvent>? {
        val sortedMap = linkedMapOf<Int, TransferEvent>()
        this.filter {
            (start && it is TransferEvent.TransferStartEvent) ||
                    (update && it is TransferEvent.TransferUpdateEvent) ||
                    (pause && it is TransferEvent.TransferPaused) ||
                    (finish && it is TransferEvent.TransferFinishEvent)
        }.forEach {
            sortedMap.remove(it.transfer.tag) //remove and then add to be sure that in case of duplicated, the new one is the last one, instead of replacing the value for existing key
            sortedMap[it.transfer.tag] = it
        }

        return sortedMap.values.toList().takeIf { it.isNotEmpty() }
    }
}