package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferAppData.RecursiveTransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.HandleAvailableOfflineEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
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
) : IHandleTransferEventUseCase {

    /**
     * Invoke.
     * @param events the [TransferEvent] that has been received.
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        val transferEvents = events.asList().takeIf { it.isNotEmpty() } ?: return

        checkOverQuota(transferEvents)
        checkBusinessAccountExpired(transferEvents)

        transferEvents.forEach { event ->
            handleAvailableOfflineEventUseCase(event)
        }

        updateInProgressTransfers(transferEvents)
        updateTransferredBytes(transferEvents)
        updateActiveTransfers(transferEvents)

        val completedEventsMap = transferEvents
            .filterNot { it.transfer.isFolderTransfer || it.transfer.isPreviewDownload() }
            .filterIsInstance<TransferEvent.TransferFinishEvent>()

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
                (it as? TransferEvent.TransferTemporaryErrorEvent)?.error is QuotaExceededMegaException
                        && it.transfer.isForeignOverQuota.not() -> true

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
        )?.map { it.transfer }?.let {
            transferRepository.updateTransferredBytes(it)
        }
    }

    private suspend fun updateActiveTransfers(events: List<TransferEvent>) {
        events.filterByType(
            start = true,
            pause = true,
            finish = true
        )?.map { transferEvent ->
            val appDataToAdd = transferEvent.transfer.folderTransferTag?.let { folderTransferTag ->
                transferRepository.getRecursiveTransferAppDataFromParent(folderTransferTag)
            }

            if (appDataToAdd.isNullOrEmpty()) {
                transferEvent.transfer
            } else {
                transferEvent.transfer.copy(
                    appData = transferEvent.transfer.appData.plus(appDataToAdd)
                )
            }
        }?.let {
            transferRepository.insertOrUpdateActiveTransfers(it)
        }
        clearParentsAppDataCache(events)
    }

    private suspend fun clearParentsAppDataCache(events: List<TransferEvent>) {
        events.filterByType(finish = true)?.forEach { transferEvent ->
            if (transferEvent.transfer.isFolderTransfer) {
                transferRepository.clearRecursiveTransferAppDataFromCache(transferEvent.transfer.tag)
            }
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
        val sortedMap = linkedMapOf<Long, TransferEvent>()
        this.filter {
            (start && it is TransferEvent.TransferStartEvent) ||
                    (update && it is TransferEvent.TransferUpdateEvent) ||
                    (pause && it is TransferEvent.TransferPaused) ||
                    (finish && it is TransferEvent.TransferFinishEvent)
        }.forEach {
            sortedMap.remove(it.transfer.uniqueId) //remove and then add to be sure that in case of duplicated, the new one is the last one, instead of replacing the value for existing key
            sortedMap[it.transfer.uniqueId] = it
        }

        return sortedMap.values.toList().takeIf { it.isNotEmpty() }
    }
}