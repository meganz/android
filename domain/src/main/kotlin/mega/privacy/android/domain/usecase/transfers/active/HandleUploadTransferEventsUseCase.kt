package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import javax.inject.Inject

/**
 * Use case to handle upload transfer events
 */
class HandleUploadTransferEventsUseCase @Inject constructor(
    private val setNodeAttributesAfterUploadUseCase: SetNodeAttributesAfterUploadUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(vararg events: TransferEvent) {
        events
            .filter { event ->
                event.transfer.transferType == TransferType.GENERAL_UPLOAD
                        && event is TransferEvent.TransferFinishEvent
                        && event.error == null
            }.mapAsync(concurrencyLimit = 5) { event ->
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