package mega.privacy.android.domain.entity.transfer.pending

import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType

data class InsertPendingTransferRequest(
    val transferType: TransferType,
    val nodeIdentifier: PendingTransferNodeIdentifier,
    val path: String,
    val appData: List<TransferAppData>?,
    val isHighPriority: Boolean,
)