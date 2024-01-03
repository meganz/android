package mega.privacy.android.app.presentation.transfers.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.data.facade.AccountInfoFacade
import mega.privacy.android.domain.usecase.quota.GetBandwidthOverQuotaDelayUseCase
import mega.privacy.android.app.fcm.CreateTransferNotificationChannelsUseCase
import mega.privacy.android.app.presentation.notifications.DownloadNotificationIntentService
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.usecase.AreCredentialsNullUseCase
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import nz.mega.sdk.MegaAccountDetails
import mega.privacy.android.icon.pack.R as iconPackR
import javax.inject.Inject

/**
 * Creates a notification to be shown when there were over quota errors while transferring in the background
 */
class DefaultOverQuotaNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isUserLoggedIn: IsUserLoggedIn,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val getBandwidthOverQuotaDelayUseCase: GetBandwidthOverQuotaDelayUseCase,
    private val areCredentialsNullUseCase: AreCredentialsNullUseCase,
    private val accountInfoFacade: AccountInfoFacade,
) : OverQuotaNotificationBuilder {

    override suspend operator fun invoke(): Notification {
        val isLoggedIn =
            isUserLoggedIn() && areCredentialsNullUseCase()
        var isFreeAccount = false
        val intent = Intent(context, DownloadNotificationIntentService::class.java)
        if (isLoggedIn) {
            isFreeAccount = accountInfoFacade.accountTypeId == MegaAccountDetails.ACCOUNT_TYPE_FREE
            intent.action = Constants.ACTION_SHOW_UPGRADE_ACCOUNT
        } else {
            clearEphemeralCredentialsUseCase()
            intent.action = Constants.ACTION_LOG_IN
        }
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = PendingIntent.getService(
            context,
            0,
            Intent(
                context.applicationContext,
                DownloadNotificationIntentService::class.java
            ),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val clickIntent =
            Intent(context, DownloadNotificationIntentService::class.java)
        clickIntent.action = Constants.ACTION_SHOW_TRANSFERS
        val clickPendingIntent = PendingIntent.getService(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val customView =
            RemoteViews(
                context.packageName,
                R.layout.notification_transfer_over_quota
            )
        customView.setTextViewText(
            R.id.content_text,
            context.getString(
                R.string.current_text_depleted_transfer_overquota,
                TimeUtils.getHumanizedTime(getBandwidthOverQuotaDelayUseCase())
            )
        )
        val dismissButtonText =
            context.getString(if (isLoggedIn) R.string.general_dismiss else R.string.login_text)
        customView.setTextViewText(R.id.dismiss_button, dismissButtonText)
        customView.setOnClickPendingIntent(R.id.dismiss_button, dismissIntent)
        val upgradeButtonText =
            context.getString(if (!isLoggedIn) R.string.continue_without_account_transfer_overquota else if (isFreeAccount) R.string.my_account_upgrade_pro else R.string.plans_depleted_transfer_overquota)
        customView.setTextViewText(R.id.upgrade_button, upgradeButtonText)
        customView.setOnClickPendingIntent(R.id.upgrade_button, pendingIntent)

        val builder = NotificationCompat.Builder(
            context,
            CreateTransferNotificationChannelsUseCase.NOTIFICATION_CHANNEL_DOWNLOAD_ID
        ).apply {
            setSmallIcon(iconPackR.drawable.ic_stat_notify)
            color = ContextCompat.getColor(context, R.color.red_600_red_300)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setContent(customView)
            setContentIntent(clickPendingIntent)
            setOngoing(false)
            setAutoCancel(true)
        }
        return builder.build()
    }
}