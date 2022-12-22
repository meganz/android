package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaTransfer

/**
 * Use Case to cancel a [MegaTransfer]
 */
fun interface CancelTransfer {

    /**
     * Cancels a [MegaTransfer]
     *
     * @param transfer the [MegaTransfer] object
     */
    suspend operator fun invoke(transfer: MegaTransfer)
}