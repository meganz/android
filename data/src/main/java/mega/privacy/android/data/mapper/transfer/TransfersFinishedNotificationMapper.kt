package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals

/**
 * Creates a final notification from ActiveTransferTotals
 */
interface TransfersFinishedNotificationMapper {
    /**
     * Creates a final notification from ActiveTransferTotals
     */
    suspend operator fun invoke(activeTransferTotals: ActiveTransferTotals): Notification
}