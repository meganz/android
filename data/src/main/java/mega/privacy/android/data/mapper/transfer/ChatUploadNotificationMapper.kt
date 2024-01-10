package mega.privacy.android.data.mapper.transfer

import android.app.Notification
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals

/**
 * Creates notification for chat uploads from ActiveTransferTotals
 */
interface ChatUploadNotificationMapper {

    /**
     * Creates notification for chat uploads from ActiveTransferTotals
     * @param activeTransferTotals the [ActiveTransferTotals] of all chatUploads
     * @param videoCompressionProgress the video compression progress if corresponds, null if there are no compression currently in progress
     * @return a notification for chat uploads
     */
    operator fun invoke(
        activeTransferTotals: ActiveTransferTotals?,
        videoCompressionProgress: VideoCompressionProgress?,
        paused: Boolean,
    ): Notification
}

/**
 * Video compression progress
 * @param alreadyCompressed the amount of videos already fully compressed
 * @param totalToCompress the total amount of videos that would need to be compressed, including the already compressed
 */
data class VideoCompressionProgress(val alreadyCompressed: Int, val totalToCompress: Int)