package mega.privacy.android.app.globalmanagement

import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.data.extensions.mapTransferType
import mega.privacy.android.domain.entity.transfer.Transfer
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer

/**
 * Data class which contains all the necessary info to manage scanning transfers.
 *
 * @property transferType   TYPE_UPLOAD if an upload, TYPE_DOWNLOAD if a download.
 * @property localPath      Path of the folder to upload if transferType is TYPE_UPLOAD.
 *                          Path where the folder will be download if transferType is TYPE_DOWNLOAD.
 * @property node           Parent MegaNode where the folder will be uploaded if transferType is TYPE_UPLOAD.
 *                          MegaNode to download if transferType is TYPE_DOWNLOAD.
 * @property isFolder       True if the transfer is a folder, false otherwise.
 * @property transferTag    MegaTransfer identifier. Only used for folder transfers.
 * @property transferStage  MegaTransfer stage. Only used for folder transfers.
 */
data class ScanningTransferData(
    val transferType: Int,
    val localPath: String,
    val node: MegaNode?,
    val isFolder: Boolean,
    var transferTag: Int = INVALID_VALUE,
    var transferStage: Long = INVALID_STAGE,
) {

    companion object {
        private const val INVALID_STAGE = -1L
    }

    /**
     * Checks if the transfer data, is the same as this ScanningTransferData.
     *
     * @param transfer  MegaTransfer to check.
     * @return True if the transfer data is the same as this ScanningFolderData, false otherwise.
     */
    fun isTheSameTransfer(transfer: MegaTransfer): Boolean =
        if (transferTag != INVALID_VALUE) transferTag == transfer.tag
        else transferType == transfer.type && isFolder == transfer.isFolderTransfer
                && (isTheSameUpload(transfer) || isTheSameDownload(transfer))

    /**
     * Checks if the transfer data, is the same as this ScanningTransferData.
     *
     * @param transfer  Transfer to check.
     * @return True if the transfer data is the same as this ScanningFolderData, false otherwise.
     */
    fun isTheSameTransfer(transfer: Transfer): Boolean =
        if (transferTag != INVALID_VALUE) transferTag == transfer.tag
        else transferType == transfer.type.mapTransferType() && isFolder == transfer.isFolderTransfer
                && (isTheSameUpload(transfer) || isTheSameDownload(transfer))

    /**
     * Checks if this is an upload and its data is the same as the received MegaTransfer.
     *
     * @param transfer  MegaTransfer to check.
     * @return True if this is an upload and its data is the same as the received MegaTransfer,
     * false otherwise.
     */
    private fun isTheSameUpload(transfer: MegaTransfer): Boolean =
        transferType == MegaTransfer.TYPE_UPLOAD
                && localPath == transfer.path
                && node?.handle == transfer.parentHandle

    /**
     * Checks if this is an upload and its data is the same as the received MegaTransfer.
     *
     * @param transfer  Transfer to check.
     * @return True if this is an upload and its data is the same as the received MegaTransfer,
     * false otherwise.
     */
    private fun isTheSameUpload(transfer: Transfer): Boolean =
        transferType == MegaTransfer.TYPE_UPLOAD
                && localPath == transfer.localPath
                && node?.handle == transfer.parentHandle

    /**
     * Checks if this is a download and its data is the same as the received MegaTransfer.
     *
     * @param transfer  MegaTransfer to check.
     * @return True if this is a download and its data is the same as the received MegaTransfer,
     * false otherwise.
     */
    private fun isTheSameDownload(transfer: MegaTransfer): Boolean =
        transferType == MegaTransfer.TYPE_DOWNLOAD
                && localPath == transfer.parentPath
                && node?.handle == transfer.nodeHandle

    /**
     * Checks if this is a download and its data is the same as the received MegaTransfer.
     *
     * @param transfer  Transfer to check.
     * @return True if this is a download and its data is the same as the received MegaTransfer,
     * false otherwise.
     */
    private fun isTheSameDownload(transfer: Transfer): Boolean =
        transferType == MegaTransfer.TYPE_DOWNLOAD
                && localPath == transfer.parentPath
                && node?.handle == transfer.nodeHandle
}
