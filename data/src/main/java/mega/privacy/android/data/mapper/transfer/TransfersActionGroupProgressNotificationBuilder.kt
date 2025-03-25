package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Creates a progress notification from ActiveTransferTotals
 */
interface TransfersActionGroupProgressNotificationBuilder {

    /**
     * Invoke.
     */
    suspend operator fun invoke(
        group: ActiveTransferTotals.Group,
        transferType: TransferType,
        paused: Boolean,
    ): Notification
}