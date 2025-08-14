package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import javax.inject.Inject

/**
 * Add (or update if already exists) an active transfer to local storage based on a TransferEvent
 *
 * @property transferRepository
 */
class HandleTransferEventUseCase @Inject internal constructor(
    private val transferRepository: TransferRepository,
    private val broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase,
    private val crashReporter: CrashReporter,
) : IHandleTransferEventUseCase {

    /**
     * Invoke.
     * @param events the [TransferEvent] that has been received.
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        events.asList().takeIf { it.isNotEmpty() }?.let { transferEvents ->
            checkPossiblePerformanceIssues(transferEvents)

            coroutineScope {
                val checkBusinessAccountDeferred =
                    async { checkBusinessAccountExpired(transferEvents) }
                val updateInProgressDeferred = async { updateInProgressTransfers(transferEvents) }
                val updateTransferredBytesDeferred =
                    async { updateTransferredBytes(transferEvents) }
                val updateActiveTransfersDeferred = async { updateActiveTransfers(transferEvents) }
                val addCompletedTransferDeferred = async { addCompletedTransfer(transferEvents) }

                checkBusinessAccountDeferred.await()
                updateInProgressDeferred.await()
                updateTransferredBytesDeferred.await()
                updateActiveTransfersDeferred.await()
                addCompletedTransferDeferred.await()
            }
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
                transferRepository.getRecursiveTransferAppDataFromParent(
                    folderTransferTag,
                    fetchInMemoryParent = { events.firstOrNull { it.transfer.tag == folderTransferTag }?.transfer }
                )
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

    private fun checkPossiblePerformanceIssues(events: List<TransferEvent>) {
        events.filterIsInstance<TransferEvent.FolderTransferUpdateEvent>()
            .filter {
                it.stage == TransferStage.STAGE_TRANSFERRING_FILES
                        && it.fileCount > POSSIBLE_PERFORMANCE_ISSUE_NUMBER
            }
            .forEach { folderUpdate ->
                val mainText =
                    if (folderUpdate.transfer.transferType == TransferType.DOWNLOAD) DOWNLOADING_FOLDER_STRING
                    else UPLOADING_FOLDER_STRING

                crashReporter.log(
                    mainText
                            + FOLDER_COUNT_STRING + folderUpdate.folderCount
                            + FILE_COUNT_STRING + folderUpdate.fileCount
                )
            }
    }

    private suspend fun addCompletedTransfer(events: List<TransferEvent>) {
        events
            .filterNot { it.transfer.isFolderTransfer || it.transfer.isPreviewDownload() }
            .filterIsInstance<TransferEvent.TransferFinishEvent>()
            .let { completedEventsMap ->
                if (completedEventsMap.isNotEmpty()) {
                    transferRepository.addCompletedTransfers(completedEventsMap)
                }
            }
    }
}

internal const val POSSIBLE_PERFORMANCE_ISSUE_NUMBER = 10000
internal const val UPLOADING_FOLDER_STRING = "Possible performance issue uploading folder."
internal const val DOWNLOADING_FOLDER_STRING = "Possible performance issue uploading folder."
internal const val FOLDER_COUNT_STRING = " Folder count: "
internal const val FILE_COUNT_STRING = ". File count: "