package mega.privacy.android.data.repository

import nz.mega.sdk.MegaTransfer

/**
 * Transfers Repository
 */
interface TransfersRepository {

    /**
     * Cancels a [MegaTransfer]
     *
     * @param transfer the [MegaTransfer] to cancel
     */
    suspend fun cancelTransfer(transfer: MegaTransfer)
}