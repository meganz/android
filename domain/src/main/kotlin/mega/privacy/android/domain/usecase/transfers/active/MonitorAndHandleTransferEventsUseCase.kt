package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.extension.collectChunked
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Use case for monitoring and handling transfer events to update [ActiveTransfer]s, [CompletedTransfer]s, etc. and other related data and metadata.
 * It filters out certain types of transfers not relevant to general transfers (voice clips, background, streaming, backup, sync)
 * and processes the remaining events in chunks for improved performance.
 *
 * It starts the corresponding transfer Worker if needed
 *
 * @property monitorTransferEventsUseCase Use case for monitoring transfer events.
 * @property handleTransferEventUseCase Use case for handling transfer events.
 */
class MonitorAndHandleTransferEventsUseCase @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val handleTransferEventUseCase: HandleTransferEventUseCase,
    private val transferRepository: TransferRepository,
) {
    /**
     * Monitors and handles transfer events of the specified type.
     *
     * @param eventsChunkDuration The duration for which events are collected into a chunks. Defaults to 2 seconds.
     * @return A [Flow] that emits the size of each processed chunk of transfer events.
     */
    operator fun invoke(eventsChunkDuration: Duration = defaultChunkDuration) =
        channelFlow {
            val workerStartSend: MutableMap<TransferType, Boolean> =
                TransferType.entries.associateWith { false }.toMutableMap()
            var lastProcessedEvent: TransferEvent? = null

            combine(
                monitorTransferEventsUseCase()
                    .filterNot { event ->
                        event.transfer.isVoiceClip()
                                || event.transfer.isBackgroundTransfer()
                                || event.transfer.isStreamingTransfer
                                || event.transfer.isBackupTransfer
                                || event.transfer.isSyncTransfer
                                || event.transfer.transferType == TransferType.CU_UPLOAD
                    },
                transferRepository.monitorIsDownloadsWorkerFinished(),
                transferRepository.monitorIsUploadsWorkerFinished(),
                transferRepository.monitorIsChatUploadsWorkerFinished(),
            ) { transferEvent, isDownloadsWorkerFinished, isUploadsWorkerFinished, isChatUploadsWorkerFinished ->
                if (transferEvent == lastProcessedEvent) return@combine null// combine also emits when changes on workers, without new events.
                lastProcessedEvent = transferEvent
                //check if worker needs to be started
                startTransferWorkerIfNeeded(
                    transferEvent,
                    mapOf(
                        TransferType.DOWNLOAD to isDownloadsWorkerFinished,
                        TransferType.GENERAL_UPLOAD to isUploadsWorkerFinished,
                        TransferType.CHAT_UPLOAD to isChatUploadsWorkerFinished,
                    ),
                    workerStartSend,
                )
                // Only transfer events are needed now
                transferEvent
            }
                .filterNotNull()
                .collectChunked(
                    chunkDuration = eventsChunkDuration,
                    flushOnIdleDuration = 200.milliseconds
                ) { transferEvents ->
                    handleTransferEventUseCase(events = transferEvents.toTypedArray())
                    send(transferEvents.size)
                }
        }

    private suspend fun startTransferWorkerIfNeeded(
        transferEvent: TransferEvent,
        isWorkerFinished: Map<TransferType, Boolean>,
        workerStartSend: MutableMap<TransferType, Boolean>,
    ) {
        val eventType = transferEvent.transfer.transferType
        //reset workerStartSend for already started workers, they need to be restarted again next time isWorkerFinished is true
        isWorkerFinished.filterValues { !it }.forEach { (t, _) ->
            workerStartSend[t] = false
        }
        //check if there's an event with no started worker and start it
        if (isWorkerFinished[eventType] == true && workerStartSend[eventType] == false) {
            when (eventType) {
                TransferType.DOWNLOAD -> transferRepository.startDownloadWorker()
                TransferType.GENERAL_UPLOAD -> transferRepository.startUploadsWorker()
                TransferType.CHAT_UPLOAD -> transferRepository.startChatUploadsWorker()
                else -> {}//no worker needed for this type
            }
            workerStartSend[eventType] = true //avoid restart if events are too close
        }
    }


    private companion object {
        /**
         * To improve performance, and avoid too much database transactions, transfer events are chunked this duration
         */
        private val defaultChunkDuration = 2000.milliseconds
    }
}