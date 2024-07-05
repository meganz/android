package mega.privacy.android.app.presentation.transfers.notification

import mega.privacy.android.icon.pack.R as iconPackR
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
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.notifications.DismissNotificationBroadcastReceiver
import mega.privacy.android.app.presentation.transfers.EXTRA_TAB
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.quota.GetBandwidthOverQuotaDelayUseCase
import nz.mega.sdk.MegaAccountDetails
import javax.inject.Inject

/**
 * Creates a notification to be shown when there were over quota errors while transferring in the background
 */
class DefaultOverQuotaNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isUserLoggedIn: IsUserLoggedIn,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val getBandwidthOverQuotaDelayUseCase: GetBandwidthOverQuotaDelayUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val accountInfoFacade: AccountInfoFacade,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : OverQuotaNotificationBuilder {

    override suspend operator fun invoke(storageOverQuota: Boolean) = if (storageOverQuota) {
        storageOverQuotaNotification()
    } else {
        transferOverQuotaNotification()
    }

    private suspend fun transferOverQuotaNotification(): Notification {
        val isLoggedIn =
            isUserLoggedIn() && hasCredentialsUseCase()
        var isFreeAccount = false
        val intent = if (isLoggedIn) {
            isFreeAccount = accountInfoFacade.accountTypeId == MegaAccountDetails.ACCOUNT_TYPE_FREE
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_SHOW_UPGRADE_ACCOUNT
            }
        } else {
            clearEphemeralCredentialsUseCase()
            Intent(context, LoginActivity::class.java).apply {
                action = Constants.ACTION_LOG_IN
                putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
            }
        }
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(
                context.applicationContext,
                DismissNotificationBroadcastReceiver::class.java
            ),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val clickIntent = if (getFeatureFlagValueUseCase(AppFeatures.TransfersSection)) {
            Intent(context, TransfersActivity::class.java).apply {
                putExtra(EXTRA_TAB, IN_PROGRESS_TAB_INDEX)
            }
        } else {
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_SHOW_TRANSFERS
                putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
            }
        }
        val clickPendingIntent = PendingIntent.getActivity(
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
            Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
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

    private fun storageOverQuotaNotification(): Notification = with(context) {
        val contentText = getString(R.string.download_show_info)
        val message = getString(R.string.overquota_alert_title)
        val intent = Intent(this, ManagerActivity::class.java)
        intent.action = Constants.ACTION_OVERQUOTA_STORAGE
        val builder = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
        ).apply {
            setSmallIcon(iconPackR.drawable.ic_stat_notify)
            color = ContextCompat.getColor(this@with, R.color.red_600_red_300)
            setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setAutoCancel(true).setTicker(contentText)
            setContentTitle(message).setContentText(contentText)
            setOngoing(false)
        }
        return builder.build()
    }
}