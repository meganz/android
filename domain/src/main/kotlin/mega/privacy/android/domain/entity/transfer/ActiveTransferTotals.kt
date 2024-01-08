package mega.privacy.android.domain.entity.transfer

/**
 * Class to expose the totals for active transfers grouped by [TransferType]
 *
 * @param transfersType [TransferType] of this totals
 * @param totalTransfers the total amount of active transfers of this type
 * @param totalFileTransfers the total amount of active file transfers of this type (paused or not)
 * @param pausedFileTransfers the total amount of paused file transfers of this type
 * @param totalFinishedTransfers the amount of current finished transfers
 * @param totalFinishedFileTransfers the amount of current finished file transfers
 * @param totalCompletedFileTransfers the amount of current completed file transfers (finished without errors)
 * @param totalBytes total bytes of all transfers of this type
 * @param transferredBytes total bytes already transferred of active transfers of this type
 * @param totalAlreadyDownloadedFiles files not downloaded because already downloaded
 */
data class ActiveTransferTotals(
    val transfersType: TransferType,
    val totalTransfers: Int,
    val totalFileTransfers: Int,
    val pausedFileTransfers: Int,
    val totalFinishedTransfers: Int,
    val totalFinishedFileTransfers: Int,
    val totalCompletedFileTransfers: Int,
    val totalBytes: Long,
    val transferredBytes: Long,
    val totalAlreadyDownloadedFiles: Int,
) {
    /**
     * @return true if there are ongoing transfers, false if all transfers are finished or there are no active transfers to transfer
     */
    fun hasOngoingTransfers() =
        totalTransfers > 0 && !hasCompleted()

    /**
     * Similar to [!isTransferring()] but in this case it will return true if there are no active transfers
     * @return true if all active transfers have finished or there are no active transfers.
     */
    fun hasCompleted() = totalFinishedTransfers == totalTransfers

    /**
     * @return true if all pending transfers are paused, false otherwise
     */
    fun allPaused() = pendingFileTransfers > 0 && pausedFileTransfers == pendingFileTransfers

    /**
     * Represents the percentage (with base 100) of the already transferred bytes
     */
    val progressPercent =
        if (totalBytes == 0L) 0 else ((transferredBytes * 100L) / totalBytes).toInt()

    /**
     * The total number of active file transfers of this specific type that are pending for download (not finished), whether paused or in progress.
     */
    val pendingFileTransfers = totalFileTransfers - totalFinishedFileTransfers

    /**
     * The total number of finished not completed file transfers (transfers with errors)
     */
    val totalFinishedWithErrorsFileTransfers =
        totalFinishedFileTransfers - totalCompletedFileTransfers - totalAlreadyDownloadedFiles
}