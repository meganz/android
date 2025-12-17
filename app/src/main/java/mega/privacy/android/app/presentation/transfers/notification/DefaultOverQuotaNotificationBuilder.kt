package mega.privacy.android.app.presentation.transfers.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.data.facade.AccountInfoFacade
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.transfers.TransfersActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.GetBandwidthOverQuotaDelayUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.getPendingIntentConsideringSingleActivity
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaAccountDetails
import javax.inject.Inject

/**
 * Creates a notification to be shown when there were over quota errors while transferring in the background
 */
class DefaultOverQuotaNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val accountInfoFacade: AccountInfoFacade,
    private val getBandwidthOverQuotaDelayUseCase: GetBandwidthOverQuotaDelayUseCase,
    private val megaNavigator: MegaNavigator,
) : OverQuotaNotificationBuilder {

    override suspend operator fun invoke(storageOverQuota: Boolean) = if (storageOverQuota) {
        storageOverQuotaNotification()
    } else {
        transferOverQuotaNotification()
    }

    private suspend fun transferOverQuotaNotification(): Notification {
        val isLoggedIn = isUserLoggedInUseCase()
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
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val clickPendingIntent = TransfersActivity.getPendingIntentForTransfersSection(
            megaNavigator,
            context,
            TransfersNavKey.Tab.Active,
        )
        val upgradeButtonText =
            context.getString(if (!isLoggedIn) sharedR.string.login_text else if (isFreeAccount) sharedR.string.general_upgrade_button else R.string.plans_depleted_transfer_overquota)

        val builder = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
        ).apply {
            setSmallIcon(iconPackR.drawable.ic_stat_notify)
            color = ContextCompat.getColor(context, R.color.red_600_red_300)
            setStyle(NotificationCompat.BigTextStyle())
            addAction(iconPackR.drawable.ic_stat_notify, upgradeButtonText, pendingIntent)
            setContentTitle(context.getString(R.string.label_transfer_over_quota))
            setContentText(
                context.getString(
                    R.string.current_text_depleted_transfer_overquota,
                    TimeUtils.getHumanizedTime(getBandwidthOverQuotaDelayUseCase().inWholeSeconds)
                )
            )
            setContentIntent(clickPendingIntent)
            setOngoing(false)
            setAutoCancel(true)
        }
        return builder.build()
    }

    private suspend fun storageOverQuotaNotification(): Notification = with(context) {
        val contentText = getString(R.string.download_show_info)
        val message = getString(R.string.overquota_alert_title)
        val pendingIntent =
            megaNavigator.getPendingIntentConsideringSingleActivity<ManagerActivity>(
            context = context,
            createPendingIntent = { intent ->
                intent.action = Constants.ACTION_OVERQUOTA_STORAGE

                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            },
            singleActivityPendingIntent = {
                MegaActivity.getPendingIntentWithExtraDestination(
                    context = applicationContext,
                    navKey = OverQuotaDialogNavKey(isOverQuota = true),
                )
            }
        )
        val builder = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
        ).apply {
            setSmallIcon(iconPackR.drawable.ic_stat_notify)
            color = ContextCompat.getColor(this@with, R.color.red_600_red_300)
            setContentIntent(
                pendingIntent
            )
            setAutoCancel(true).setTicker(contentText)
            setContentTitle(message).setContentText(contentText)
            setOngoing(false)
        }
        return builder.build()
    }
}