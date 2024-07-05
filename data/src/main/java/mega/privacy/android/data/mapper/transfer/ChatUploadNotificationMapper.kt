package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress

/**
 * Creates notification for chat uploads from ActiveTransferTotals
 */
interface ChatUploadNotificationMapper {

    /**
     * Creates notification for chat uploads from ActiveTransferTotals
     * @param activeTransferTotals the [ActiveTransferTotals] of all chatUploads
     * @param chatCompressionProgress the video compression progress if corresponds, null if there are no compression currently in progress
     * @return a notification for chat uploads
     */
    suspend operator fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        chatCompressionProgress: ChatCompressionProgress?,
        paused: Boolean,
    ): Notification
}