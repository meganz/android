package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress

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
 * @param totalAlreadyTransferredFiles files not downloaded because already downloaded
 * @param totalCancelled files canceled before transfer has completed
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
    val totalAlreadyTransferredFiles: Int,
    val totalCancelled: Int,
    val groups: List<Group> = emptyList(),
) {
    /**
     * Information about transfer groups. A transfer group represents all the transfers initiated by a single user action.
     * @param groupId
     * @param totalFiles the total files that will be transferred, notice that this is not the total files selected by the user, as it includes child files in case of folder transfers
     * @param finishedFiles the amount of files that have finished
     * @param completedFiles the amount of files completed (finished without errors)
     * @param alreadyTransferred the amount of files not transferred because already transferred
     * @param destination the destination of the transfer
     * @param singleFileName in case of a single file transfer, the name of the file, null otherwise
     */
    data class Group(
        val groupId: Int,
        val totalFiles: Int,
        val finishedFiles: Int,
        val completedFiles: Int,
        val alreadyTransferred: Int,
        val destination: String,
        val singleFileName: String?,
    ) {
        fun finished() = totalFiles == finishedFiles
        val finishedFilesWithErrors = finishedFiles - completedFiles - alreadyTransferred
    }

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
     * Represents the progress of the already transferred bytes
     */
    val transferProgress = Progress(transferredBytes, totalBytes)

    /**
     * The total number of active file transfers of this specific type that are pending for download (not finished), whether paused or in progress.
     */
    val pendingFileTransfers = totalFileTransfers - totalFinishedFileTransfers

    /**
     * The total number of finished not completed file transfers (transfers with errors)
     */
    val totalFinishedWithErrorsFileTransfers =
        totalFinishedFileTransfers - totalCompletedFileTransfers - totalAlreadyTransferredFiles
}