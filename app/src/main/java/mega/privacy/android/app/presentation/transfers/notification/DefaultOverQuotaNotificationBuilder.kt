package mega.privacy.android.app.presentation.transfers.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.GetBandwidthOverQuotaDelayUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.LoginNavKey
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey
import mega.privacy.android.navigation.destination.QuotaWarningUpgradeNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Creates a notification to be shown when there were over quota errors while transferring in the background
 */
class DefaultOverQuotaNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
    private val getBandwidthOverQuotaDelayUseCase: GetBandwidthOverQuotaDelayUseCase,
    private val megaNavigator: MegaNavigator,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : OverQuotaNotificationBuilder {

    override suspend operator fun invoke(storageOverQuota: Boolean) = if (storageOverQuota) {
        storageOverQuotaNotification()
    } else {
        transferOverQuotaNotification()
    }

    private suspend fun transferOverQuotaNotification(): Notification {
        val isLoggedIn = isUserLoggedInUseCase()
        var isFreeAccount = false
        val actionPendingIntent = if (isLoggedIn) {
            isFreeAccount = getAccountTypeUseCase() == AccountType.FREE
            megaNavigator.getPendingIntentWithDestination(
                context = context,
                singleActivityDestination = { UpgradeAccountNavKey(source = UpgradeAccountSource.UNKNOWN) }
            )
        } else {
            clearEphemeralCredentialsUseCase()
            megaNavigator.getPendingIntentWithDestination(
                context = context,
                singleActivityDestination = { LoginNavKey(action = Constants.ACTION_LOG_IN) }
            )
        }
        val clickPendingIntent = megaNavigator.getPendingIntentWithDestination(
            context = context,
            singleActivityDestination = { TransfersNavKey(TransfersNavKey.Tab.Active) }
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
            addAction(iconPackR.drawable.ic_stat_notify, upgradeButtonText, actionPendingIntent)
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
        val useUpsell = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.QuotaWarningUpsellScreen)
        }.getOrDefault(false)
        val pendingIntent = if (useUpsell) {
            megaNavigator.getPendingIntentWithDestination(
                context = context,
                singleActivityDestination = {
                    QuotaWarningUpgradeNavKey(
                        type = QuotaWarningType.Storage,
                        trigger = QuotaWarningTrigger.Upload,
                    )
                }
            )
        } else {
            megaNavigator.getPendingIntentWithDestination(
                context = context,
                singleActivityDestination = { OverQuotaDialogNavKey(isOverQuota = true) }
            )
        }
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