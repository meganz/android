package mega.privacy.android.data.model

import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

/**
 * Global transfer
 *
 * @property transfer
 */
sealed interface GlobalTransfer {
    val transfer: MegaTransfer

    /**
     * On transfer start
     *
     * @property transfer
     */
    data class OnTransferStart(override val transfer: MegaTransfer) : GlobalTransfer

    /**
     * On transfer finish
     *
     * @property transfer
     * @property error
     */
    data class OnTransferFinish(override val transfer: MegaTransfer, val error: MegaError) :
        GlobalTransfer

    /**
     * On transfer update
     *
     * @property transfer
     */
    data class OnTransferUpdate(override val transfer: MegaTransfer) : GlobalTransfer

    /**
     * On transfer temporary error
     *
     * @property transfer
     * @property error
     */
    data class OnTransferTemporaryError(
        override val transfer: MegaTransfer,
        val error: MegaError,
    ) : GlobalTransfer

    /**
     * On transfer data
     *
     * @property transfer
     * @property buffer
     */
    data class OnTransferData(override val transfer: MegaTransfer, val buffer: ByteArray?) :
        GlobalTransfer

    /**
     * On folder transfer update
     *
     * @property transfer
     * @property stage
     * @property folderCount
     * @property createdFolderCount
     * @property fileCount
     * @property currentFolder
     * @property currentFileLeafName
     */
    data class OnFolderTransferUpdate(
        override val transfer: MegaTransfer,
        val stage: Int,
        val folderCount: Long,
        val createdFolderCount: Long,
        val fileCount: Long,
        val currentFolder: String?,
        val currentFileLeafName: String?,
    ) : GlobalTransfer
}