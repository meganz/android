package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals

/**
 * Creates a notification for transfers from ActiveTransferTotals
 */
interface TransfersNotificationMapper {

    /**
     * Invoke.
     */
    suspend operator fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        paused: Boolean,
    ): Notification
}