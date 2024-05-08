package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.pendingMessageIds
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import javax.inject.Inject

/**
 * Use case to monitor pending message transfer events.
 */
class MonitorPendingMessageTransferEventsUseCase @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
) {

    /**
     * Invoke
     */
    operator fun invoke() = monitorTransferEventsUseCase()
        .filterIsInstance<TransferEvent.TransferUpdateEvent>()
        .mapNotNull { event ->
            event.transfer.pendingMessageIds()
                ?.let { it to event.transfer }
        }
        .onStart {
            getInProgressTransfersUseCase().filter {
                it.pendingMessageIds() != null
            }.forEach { transfer ->
                transfer.pendingMessageIds()?.let {
                    emit(it to transfer)
                }
            }
        }
}