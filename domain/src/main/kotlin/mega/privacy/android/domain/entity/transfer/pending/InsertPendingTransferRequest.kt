package mega.privacy.android.domain.entity.transfer.pending

import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * @param transferType
 * @param nodeIdentifier
 * @param uriPath the [UriPath] of the file or folder to be uploaded from or downloaded to
 * @param appData
 * @param isHighPriority
 * @param fileName The name of the file to be shown in completed Transfers. It can be used to rename uploaded nodes. Current file name will be used if not specified.
 */
data class InsertPendingTransferRequest(
    val transferType: TransferType,
    val nodeIdentifier: PendingTransferNodeIdentifier,
    val uriPath: UriPath,
    val appData: List<TransferAppData> = emptyList(),
    val isHighPriority: Boolean,
    val fileName: String?,
)