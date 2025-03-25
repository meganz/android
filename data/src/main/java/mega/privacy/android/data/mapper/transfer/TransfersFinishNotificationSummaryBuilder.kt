package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Interface to build the finish summary notification for transfers.
 */
interface TransfersFinishNotificationSummaryBuilder {

    /**
     * Invoke
     */
    suspend operator fun invoke(type: TransferType): Notification
}