package mega.privacy.android.app.globalmanagement

import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_FOLDER_DIALOG
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.shouldUpdateScanningFolderDialog
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD
import nz.mega.sdk.MegaTransfer.TYPE_UPLOAD

/**
 * Data class which contains all the necessary data to manage scanning folders.
 *
 * @property cancelToken    MegaCancelToken to cancel the transfer if needed.
 * @property transferType   TYPE_UPLOAD if an upload, TYPE_DOWNLOAD if a download.
 * @property localPath      Path of the folder to upload if transferType is TYPE_UPLOAD.
 *                          Path where the folder will be download if transferType is TYPE_DOWNLOAD.
 * @property node           Parent MegaNode where the folder will be uploaded if transferType is TYPE_UPLOAD.
 *                          MegaNode to download if transferType is TYPE_DOWNLOAD.
 * @property transferTag    MegaTransfer identifier.
 * @property transferStage  MegaTransfer stage.
 */
data class ScanningFolderData(
    val cancelToken: MegaCancelToken,
    val transferType: Int,
    val localPath: String,
    val node: MegaNode,
    var transferTag: Int = INVALID_VALUE,
    var transferStage: Long = INVALID_VALUE.toLong()
) {
    /**
     * Checks if the transfer data related to the start request, is the same as this ScanningFolderData.
     *
     * @param transfer  MegaTransfer to check.
     * @return True if the transfer data is the same as this ScanningFolderData, false otherwise.
     */
    fun isTheSameScanningFolder(transfer: MegaTransfer): Boolean =
        transferType == transfer.type && (isTheSameUpload(transfer) || isTheSameDownload(transfer))

    /**
     * Checks if this is an upload and its data is the same as the received MegaTransfer.
     *
     * @param transfer  MegaTransfer to check.
     * @return True if this is an upload and its data is the same as the received MegaTransfer,
     * false otherwise.
     */
    private fun isTheSameUpload(transfer: MegaTransfer): Boolean =
        transferType == TYPE_UPLOAD
                && localPath == transfer.path
                && node.handle == transfer.parentHandle

    /**
     * Checks if this is a download and its data is the same as the received MegaTransfer.
     *
     * @param transfer  MegaTransfer to check.
     * @return True if this is a download and its data is the same as the received MegaTransfer,
     * false otherwise.
     */
    private fun isTheSameDownload(transfer: MegaTransfer): Boolean =
        transferType == TYPE_DOWNLOAD
                && localPath == transfer.parentPath
                && node.handle == transfer.nodeHandle

    /**
     * Checks if isTheSameScanningFolder and ensures it by checking if the transfer tag is the same.
     *
     * @param transfer MegaTransfer to check.
     * @return True if isTheSameScanningFolder and the transfer tag is the same, false otherwise.
     */
    fun isTheSameScanningTransfer(transfer: MegaTransfer): Boolean =
        isTheSameScanningFolder(transfer) && transferTag == transfer.tag

    /**
     * Updates:
     *  - The transferTag and transferStage if they have not been initialized yet.
     *  - The transferStage if is not up to date.
     *  - Nothing if none of the above is true.
     *
     *  Launches a LiveEventBus to update the scanning folder dialog if something has been updated
     *  and if the transferStage requires it.
     *
     * @param transfer MegaTransfer to check and update.
     */
    fun performUpdates(transfer: MegaTransfer) {
        when {
            transferTag == INVALID_VALUE -> initTransferTagAndStage(transfer)
            transferTag == transfer.tag && transferStage != transfer.stage -> transferStage =
                transfer.stage
            else -> return
        }

        if (shouldUpdateScanningFolderDialog(transfer)) {
            LiveEventBus.get(EVENT_SHOW_SCANNING_FOLDER_DIALOG, MegaTransfer::class.java)
                .post(transfer)
        }
    }

    /**
     * Initializes transferTag and transferStage after the first transfer update.
     *
     * @param transfer MegaTransfer to update.
     */
    private fun initTransferTagAndStage(transfer: MegaTransfer) {
        transferTag = transfer.tag
        transferStage = transfer.stage
    }
}
