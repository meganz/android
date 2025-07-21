package mega.privacy.android.app.presentation.transfers.starttransfer.model

import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Info to ask confirmation for large downloads
 * @param sizeString: the size of the download
 * @param transferTriggerEvent: the event to start again the download if confirmed
 */
data class ConfirmLargeDownloadInfo(
    val sizeString: String,
    val transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent,
)