package mega.privacy.android.app.presentation.transfers.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.TransfersFinishNotificationSummaryBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.finalSummaryGroup
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.icon.pack.R as iconPackR
import javax.inject.Inject

/**
 * Default implementation of [TransfersFinishNotificationSummaryBuilder]
 */
class DefaultTransfersFinishNotificationSummaryBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
) : TransfersFinishNotificationSummaryBuilder {

    override suspend fun invoke(type: TransferType) =
        NotificationCompat.Builder(
            context,
            when (type) {
                TransferType.DOWNLOAD -> Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                TransferType.GENERAL_UPLOAD -> Constants.NOTIFICATION_CHANNEL_UPLOAD_ID
                else -> throw IllegalArgumentException("Invalid transfer type: $type")
            }
        )
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setGroup(finalSummaryGroup(type))
            .setGroupSummary(true)
            .setContentIntent(createPendingIntent(TransfersActivity.getCompletedTabIntent(context)))
            .build()

    private fun createPendingIntent(intent: Intent) =
        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}