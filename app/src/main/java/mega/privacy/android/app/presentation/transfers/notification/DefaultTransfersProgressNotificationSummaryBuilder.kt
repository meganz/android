package mega.privacy.android.app.presentation.transfers.notification

import android.app.PendingIntent
import mega.privacy.android.icon.pack.R as iconPackR
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.TransfersProgressNotificationSummaryBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.PROGRESS_SUMMARY_GROUP
import mega.privacy.android.domain.entity.transfer.TransferType
import javax.inject.Inject

/**
 * Default implementation of [TransfersProgressNotificationSummaryBuilder]
 */
class DefaultTransfersProgressNotificationSummaryBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openTransfersSectionIntentMapper: OpenTransfersSectionIntentMapper,
) : TransfersProgressNotificationSummaryBuilder {

    override suspend fun invoke(type: TransferType) =
        NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setGroup(PROGRESS_SUMMARY_GROUP + type.name)
            .setGroupSummary(true)
            .setContentTitle(context.getString(R.string.download_preparing_files))
            .setContentIntent(createPendingIntent(openTransfersSectionIntentMapper(TransfersTab.PENDING_TAB)))
            .build()

    private fun createPendingIntent(intent: Intent) =
        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}