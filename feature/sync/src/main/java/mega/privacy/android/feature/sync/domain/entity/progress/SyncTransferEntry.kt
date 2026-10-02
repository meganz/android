package mega.privacy.android.feature.sync.domain.entity.progress

import mega.privacy.android.domain.entity.transfer.TransferState

/** One sync transfer as the SDK reported it, keyed by [tag]. */
internal data class SyncTransferEntry(
    val tag: Int,
    val direction: SyncTransferDirection,
    val state: TransferState,
    val fileName: String,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speed: Long,
    val meanSpeed: Long,
    val priority: Long,
    val notificationNumber: Long,
    val lastError: SyncTransferError?,
    val isForeignOverQuota: Boolean,
    val path: String?,
)
