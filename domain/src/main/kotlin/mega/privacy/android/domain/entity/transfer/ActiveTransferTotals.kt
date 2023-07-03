package mega.privacy.android.domain.entity.transfer

/**
 * Class to expose the totals for active transfers grouped by [TransferType]
 *
 * @param transfersType [TransferType] of this totals
 * @param totalTransfers the total amount of active transfers of this type
 * @param totalFinishedTransfers the amount of current finished transfers
 * @param totalBytes total bytes of all transfers of this type
 * @param transferredBytes total bytes already transferred of active transfers of this type
 */
data class ActiveTransferTotals(
    val transfersType: TransferType,
    val totalTransfers: Int,
    val totalFinishedTransfers: Int,
    val totalBytes: Long,
    val transferredBytes: Long,
)