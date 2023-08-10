package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals

/**
 * Creates a notification from ActiveTransferTotals
 */
interface DownloadNotificationMapper {

    /**
     * Creates a notification from ActiveTransferTotals
     */
    operator fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        paused: Boolean,
    ): Notification
}