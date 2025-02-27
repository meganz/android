package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress
import kotlin.time.Duration.Companion.milliseconds

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
     * @param singleTransferTag in case of a single file transfer, its, null otherwise
     * @param startTime the local time in milliseconds when this action was started, it should be used for UX only as precision is not guaranteed
     * @param pausedFiles the amount of files that are paused
     * @param totalBytes the total bytes of all files in this group
     * @param transferredBytes the total bytes already transferred of all files in this group
     * @param appData the list of app data of the transfers in this group. Group app data itself is filtered out. Only one instance of each app data type is added to represent this group.
     */
    data class Group(
        val groupId: Int,
        val totalFiles: Int,
        val finishedFiles: Int,
        val completedFiles: Int,
        val alreadyTransferred: Int,
        val destination: String,
        val singleFileName: String?,
        val singleTransferTag: Int?,
        val startTime: Long,
        val pausedFiles: Int,
        val totalBytes: Long,
        val transferredBytes: Long,
        override val appData: List<TransferAppData> = emptyList(),
    ) : AppDataOwner {

        /**
         * @return true if all files in this group have finished, false otherwise
         */
        fun finished() = totalFiles == finishedFiles

        /**
         * The amount of files that have finished but are not completed (finished with errors)
         */
        val finishedFilesWithErrors = finishedFiles - completedFiles - alreadyTransferred

        fun durationFromStart(currentTimeInMillis: Long) =
            (currentTimeInMillis - startTime).milliseconds

        /**
         * @return true if all pending files in this group are paused, false otherwise
         */
        fun allPaused() = totalFiles - finishedFiles == pausedFiles

        /**
         * The progress of the already transferred bytes
         */
        val progress = Progress(transferredBytes, totalBytes)
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