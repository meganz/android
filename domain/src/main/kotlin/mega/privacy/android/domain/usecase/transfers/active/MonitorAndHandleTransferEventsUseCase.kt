package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNot
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.extension.collectChunked
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Use case to create a flow that monitors and handles transfer events to update [ActiveTransfer]s, [CompletedTransfer]s, etc. and other related data and metadata.
 * It filters out certain types of transfers not relevant to general transfers (voice clips, background, streaming, backup, sync)
 * and processes the remaining events in chunks for improved performance.
 *
 * @property monitorTransferEventsUseCase Use case for monitoring transfer events.
 * @property handleTransferEventsUseCases Use case for handling transfer events.
 */
class MonitorAndHandleTransferEventsUseCase @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val handleTransferEventsUseCases: Set<@JvmSuppressWildcards IHandleTransferEventUseCase>,
) {
    /**
     * Monitors and handles transfer events of the specified type.
     *
     * @param eventsChunkDuration The duration for which events are collected into a chunks. Defaults to 2 seconds.
     * @return A [Flow] that emits the size of each processed chunk of transfer events.
     */
    operator fun invoke(eventsChunkDuration: Duration = defaultChunkDuration) =
        channelFlow {
            monitorTransferEventsUseCase()
                .filterNot { event ->
                    event.transfer.isVoiceClip()
                            || event.transfer.isBackgroundTransfer()
                            || event.transfer.isStreamingTransfer
                            || event.transfer.isBackupTransfer
                            || event.transfer.isSyncTransfer
                            || event.transfer.transferType == TransferType.CU_UPLOAD
                }
                .collectChunked(
                    chunkDuration = eventsChunkDuration,
                    flushOnIdleDuration = 200.milliseconds
                ) {
                    val transferEvents = it.toTypedArray()
                    handleTransferEventsUseCases.forEach {
                        it(events = transferEvents)
                    }
                    send(transferEvents.size)
                }
        }


    private companion object {
        /**
         * To improve performance, and avoid too much database transactions, transfer events are chunked this duration
         */
        private val defaultChunkDuration = 2000.milliseconds
    }
}