package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import javax.inject.Inject

/**
 * Use case to handle download transfer events
 */
class HandleDownloadTransferEventsUseCase @Inject constructor(
    private val scanMediaFileUseCase: ScanMediaFileUseCase,
) : IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        events.filter { event ->
            event.transfer.transferType == TransferType.DOWNLOAD
                    && event is TransferEvent.TransferFinishEvent
                    && event.transfer.transferredBytes == event.transfer.totalBytes
                    && event.error == null
        }.map { it.transfer.localPath }
            .takeIf { it.isNotEmpty() }
            ?.let { paths ->
                runCatching {
                    scanMediaFileUseCase(paths.toTypedArray(), arrayOf(""))
                }
            }
    }
}