package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier

/**
 * Represents an active transfer group, all transfers started by a single user action.
 *
 * @property groupId The id of this group
 * @property transferType The [TransferType] of the transfers in this group
 * @property destination The destination of the transfers in this group, UriPath value in case of downloads
 * @property startTime the local time in milliseconds when this action was started, it should be used for UX only as precision is not guaranteed7
 * @property pendingTransferNodeId the destination of this transfer in case of folder node destination, null otherwise
 */
interface ActiveTransferActionGroup {
    val groupId: Int?
    val transferType: TransferType
    val destination: String
    val startTime: Long?
    val pendingTransferNodeId: PendingTransferNodeIdentifier?
}