package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Creates a final notification from ActiveTransferTotals
 */
interface TransfersGroupFinishNotificationBuilder {
    /**
     * Creates a final notification from ActiveTransferTotals
     */
    suspend operator fun invoke(
        group: ActiveTransferTotals.Group,
        transferType: TransferType,
    ): Notification
}