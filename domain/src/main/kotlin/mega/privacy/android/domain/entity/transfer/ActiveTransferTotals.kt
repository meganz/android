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
) {
    /**
     * @return true if there are ongoing transfers, false if all transfers are finished or there are no active transfers to transfer
     */
    fun hasOngoingTransfers() =
        totalBytes > 0 && totalTransfers > 0 && transferredBytes < totalBytes

    /**
     * Similar to [!isTransferring()] but in this case it will return true if there are no active transfers
     * @return true if all active transfers have finished or there are no active transfers.
     */
    fun hasCompleted() = transferredBytes == totalBytes

    /**
     * Represents the percentage (with base 100) of the already transferred bytes
     */
    val progressPercent by lazy { if (totalBytes == 0L) 0 else ((transferredBytes * 100L) / totalBytes).toInt() }
}