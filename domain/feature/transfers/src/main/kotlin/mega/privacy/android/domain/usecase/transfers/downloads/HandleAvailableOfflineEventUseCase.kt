package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflineTransferUseCase
import mega.privacy.android.domain.usecase.offline.SaveOfflineNodeInformationUseCase
import javax.inject.Inject

/**
 * Handles transfer events in case the event is related to Save available offline:
 * - When a transfer is finished without errors, the offline information is saved to the database
 */
class HandleAvailableOfflineEventUseCase @Inject constructor(
    private val isOfflineTransferUseCase: IsOfflineTransferUseCase,
    private val saveOfflineNodeInformationUseCase: SaveOfflineNodeInformationUseCase,
    private val broadcastOfflineFileAvailabilityUseCase: BroadcastOfflineFileAvailabilityUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(event: TransferEvent) {
        if (event is TransferEvent.TransferFinishEvent && event.transfer.transferType == TransferType.DOWNLOAD && event.error == null) {
            // saveOfflineNodeInformationUseCase takes a while and the worker can cancel the job, so we need to launch it in a non cancellable context
            withContext(NonCancellable) {
                launch {
                    if (event.transfer.isOfflineDownload()
                        || (!event.transfer.isRootTransfer && isOfflineTransferUseCase(event.transfer)) //app data is not copied to children transfers yet
                    ) {
                        event.transfer.nodeHandle.takeIf { it != -1L }?.let {
                            saveOfflineNodeInformationUseCase(event)
                            broadcastOfflineFileAvailabilityUseCase(it)
                        }
                    }
                }
            }
        }
    }
}