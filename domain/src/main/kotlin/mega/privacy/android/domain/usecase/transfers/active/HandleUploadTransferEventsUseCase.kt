package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import javax.inject.Inject

/**
 * Use case to handle upload transfer events
 */
class HandleUploadTransferEventsUseCase @Inject constructor(
    private val setNodeAttributesAfterUploadUseCase: SetNodeAttributesAfterUploadUseCase,
) : IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        // setNodeAttributesAfterUploadUseCase takes a while and the worker can cancel the job, so we need to launch it in a non cancellable context
        withContext(NonCancellable) {
            launch {
                events
                    .filter { event ->
                        (event.transfer.transferType == TransferType.GENERAL_UPLOAD || event.transfer.transferType == TransferType.CHAT_UPLOAD)
                                && event is TransferEvent.TransferFinishEvent
                                && event.error == null
                    }.forEach { event ->
                        runCatching {
                            setNodeAttributesAfterUploadUseCase(
                                nodeHandle = event.transfer.nodeHandle,
                                uriPath = UriPath(event.transfer.localPath),
                                appData = event.transfer.appData
                            )
                        }
                    }
            }
        }
    }
}