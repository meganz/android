package mega.privacy.android.app.presentation.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.AreCredentialsNullUseCase
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid

@Deprecated(
    "There's an equivalent use case for that",
    ReplaceWith("OverQuotaNotificationBuilder")
)
internal object TransferOverQuotaNotification {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TransferOverQuotaNotificationEntryPoint {

        @ApplicationScope
        fun applicationScope(): CoroutineScope

        @MegaApi
        fun megaApi(): MegaApiAndroid
        fun isUserLoggedIn(): IsUserLoggedIn
        fun areCredentialsNullUseCase(): AreCredentialsNullUseCase
        fun clearEphemeralCredentialsUseCase(): ClearEphemeralCredentialsUseCase
    }

    fun show(applicationContext: Context) {
        val entryPoint: TransferOverQuotaNotificationEntryPoint =
            EntryPointAccessors.fromApplication(applicationContext)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        entryPoint.applicationScope().launch {
            val isLoggedIn =
                entryPoint.isUserLoggedIn().invoke()
                        && entryPoint.areCredentialsNullUseCase().invoke()
            var isFreeAccount = false
            val intent = Intent(applicationContext, DownloadNotificationIntentService::class.java)
            if (isLoggedIn) {
                isFreeAccount =
                    MegaApplication.getInstance().myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE
                intent.action = Constants.ACTION_SHOW_UPGRADE_ACCOUNT
            } else {
                entryPoint.clearEphemeralCredentialsUseCase().invoke()
                intent.action = Constants.ACTION_LOG_IN
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val dismissIntent = PendingIntent.getService(
                applicationContext,
                0,
                Intent(
                    applicationContext.applicationContext,
                    DownloadNotificationIntentService::class.java
                ),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val clickIntent =
                Intent(applicationContext, DownloadNotificationIntentService::class.java)
            clickIntent.action = Constants.ACTION_SHOW_TRANSFERS
            val clickPendingIntent = PendingIntent.getService(
                applicationContext,
                0,
                clickIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val customView =
                RemoteViews(
                    applicationContext.packageName,
                    R.layout.notification_transfer_over_quota
                )
            customView.setTextViewText(
                R.id.content_text,
                applicationContext.getString(
                    R.string.current_text_depleted_transfer_overquota,
                    TimeUtils.getHumanizedTime(entryPoint.megaApi().bandwidthOverquotaDelay)
                )
            )
            val dismissButtonText =
                applicationContext.getString(if (isLoggedIn) R.string.general_dismiss else R.string.login_text)
            customView.setTextViewText(R.id.dismiss_button, dismissButtonText)
            customView.setOnClickPendingIntent(R.id.dismiss_button, dismissIntent)
            val upgradeButtonText =
                applicationContext.getString(if (!isLoggedIn) R.string.continue_without_account_transfer_overquota else if (isFreeAccount) R.string.my_account_upgrade_pro else R.string.plans_depleted_transfer_overquota)
            customView.setTextViewText(R.id.upgrade_button, upgradeButtonText)
            customView.setOnClickPendingIntent(R.id.upgrade_button, pendingIntent)
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
            val builderCompat = NotificationCompat.Builder(
                applicationContext,
                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
            )
            builderCompat.setSmallIcon(iconPackR.drawable.ic_stat_notify)
                .setColor(ContextCompat.getColor(applicationContext, R.color.red_600_red_300))
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContent(customView)
                .setContentIntent(clickPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
            notificationManager.notify(
                Constants.NOTIFICATION_DOWNLOAD_FINAL,
                builderCompat.build()
            )

        }
    }
}