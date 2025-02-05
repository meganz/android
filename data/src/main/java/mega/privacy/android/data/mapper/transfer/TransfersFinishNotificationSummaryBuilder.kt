package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.TransferType

interface TransfersFinishNotificationSummaryBuilder {
    suspend operator fun invoke(type: TransferType): Notification
}