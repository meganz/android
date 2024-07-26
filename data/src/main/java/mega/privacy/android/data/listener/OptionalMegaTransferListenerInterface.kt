package mega.privacy.android.data.listener

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface

/**
 * MegaRequestListenerInterface with optional callbacks.
 */
internal class OptionalMegaTransferListenerInterface(
    private val onTransferStart: ((MegaTransfer) -> Unit)? = null,
    private val onTransferFinish: ((MegaTransfer, MegaError) -> Unit)? = null,
    private val onTransferUpdate: ((MegaTransfer) -> Unit)? = null,
    private val onTransferTemporaryError: ((MegaTransfer, MegaError) -> Unit)? = null,
    private val onTransferData: ((MegaTransfer, ByteArray) -> Unit)? = null,
    private val onFolderTransferUpdate: ((
        transfer: MegaTransfer,
        stage: Int,
        folderCount: Long,
        createdFolderCount: Long,
        fileCount: Long,
        currentFolder: String?,
        currentFileLeafName: String?,
    ) -> Unit)? = null,
) : MegaTransferListenerInterface {

    override fun onTransferStart(
        api: MegaApiJava,
        transfer: MegaTransfer,
    ) {
        onTransferStart?.invoke(transfer)
    }

    override fun onTransferFinish(
        api: MegaApiJava,
        transfer: MegaTransfer,
        e: MegaError,
    ) {
        onTransferFinish?.invoke(transfer, e)
    }

    override fun onTransferUpdate(
        api: MegaApiJava,
        transfer: MegaTransfer,
    ) {
        onTransferUpdate?.invoke(transfer)
    }

    override fun onTransferTemporaryError(
        api: MegaApiJava,
        transfer: MegaTransfer,
        e: MegaError,
    ) {
        onTransferTemporaryError?.invoke(transfer, e)
    }

    override fun onTransferData(
        api: MegaApiJava,
        transfer: MegaTransfer,
        buffer: ByteArray,
    ): Boolean {
        onTransferData?.invoke(transfer, buffer)
        return onTransferData != null
    }

    override fun onFolderTransferUpdate(
        api: MegaApiJava,
        transfer: MegaTransfer,
        stage: Int,
        folderCount: Long,
        createdFolderCount: Long,
        fileCount: Long,
        currentFolder: String?,
        currentFileLeafName: String?,
    ) {
        onFolderTransferUpdate?.invoke(
            transfer,
            stage,
            folderCount,
            createdFolderCount,
            fileCount,
            currentFolder,
            currentFileLeafName
        )
    }
}