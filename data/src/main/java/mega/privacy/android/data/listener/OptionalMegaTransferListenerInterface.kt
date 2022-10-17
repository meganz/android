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
    private val onTransferData: ((MegaTransfer, ByteArray) -> Unit)? = null
) : MegaTransferListenerInterface {

    override fun onTransferStart(
        api: MegaApiJava,
        transfer: MegaTransfer
    ) {
        onTransferStart?.invoke(transfer)
    }

    override fun onTransferFinish(
        api: MegaApiJava,
        transfer: MegaTransfer,
        error: MegaError
    ) {
        onTransferFinish?.invoke(transfer, error)
    }

    override fun onTransferUpdate(
        api: MegaApiJava,
        transfer: MegaTransfer
    ) {
        onTransferUpdate?.invoke(transfer)
    }

    override fun onTransferTemporaryError(
        api: MegaApiJava,
        transfer: MegaTransfer,
        error: MegaError
    ) {
        onTransferTemporaryError?.invoke(transfer, error)
    }

    override fun onTransferData(
        api: MegaApiJava,
        transfer: MegaTransfer,
        buffer: ByteArray
    ): Boolean {
        onTransferData?.invoke(transfer, buffer)
        return onTransferData != null
    }
}