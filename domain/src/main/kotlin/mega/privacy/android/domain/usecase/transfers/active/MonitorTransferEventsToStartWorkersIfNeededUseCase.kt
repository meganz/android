package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filterNot
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import javax.inject.Inject

/**
 * Use case to create a flow that monitors transfer events to start the relevant transfer workers
 *
 *
 * @property monitorTransferEventsUseCase Use case for monitoring transfer events.
 */
class MonitorTransferEventsToStartWorkersIfNeededUseCase @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val transferRepository: TransferRepository,
) {
    /**
     * Monitors and handles transfer events of the specified type.
     *
     * @return A [Flow] that emits the size of each processed chunk of transfer events.
     */
    operator fun invoke() =
        channelFlow {
            val workerStartSend: MutableMap<TransferType, Boolean> =
                TransferType.entries.associateWith { false }.toMutableMap()
            var lastProcessedEvent: TransferEvent? = null
            combineTransform(
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
                if (transferEvent == lastProcessedEvent) return@combineTransform// combine also emits when changes on workers, without new events.
                lastProcessedEvent = transferEvent

                //check if worker needs to be started
                val workerStarted = startTransferWorkerIfNeeded(
                    transferEvent,
                    mapOf(
                        TransferType.DOWNLOAD to isDownloadsWorkerFinished,
                        TransferType.GENERAL_UPLOAD to isUploadsWorkerFinished,
                        TransferType.CHAT_UPLOAD to isChatUploadsWorkerFinished,
                    ),
                    workerStartSend,
                )
                if (workerStarted) emit(transferEvent.transfer.transferType)
            }.collect {
                send(it)
            }
        }

    private suspend fun startTransferWorkerIfNeeded(
        transferEvent: TransferEvent,
        isWorkerFinished: Map<TransferType, Boolean>,
        workerStartSend: MutableMap<TransferType, Boolean>,
    ): Boolean {
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
            //avoid restart if events are too close and worker state is not updated yet
            workerStartSend[eventType] = true
            return true
        }
        return false
    }
}