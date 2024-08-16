package mega.privacy.android.app.notifications

import mega.privacy.android.icon.pack.R as iconPackR
import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_PROMO_ID
import mega.privacy.android.domain.entity.pushes.PushMessage.PromoPushMessage
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager class to show a device Notification given a [PromoPushMessage]
 *
 * @property notificationManagerCompat    [NotificationManagerCompat]
 *
 */
class PromoPushNotificationManager @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
) {

    /**
     * Show Notification given a [PromoPushMessage]
     * @param context       [Context]
     * @param pushMessage   [PromoPushMessage]
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(context: Context, pushMessage: PromoPushMessage) {

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pushMessage.redirectLink))
        val pendingIntent = if (intent.resolveActivity(context.packageManager) != null) {
            PendingIntent.getActivity(
                context,
                pushMessage.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            Timber.d("No Application found to can handle promo notification intent")
            PendingIntent.getActivity(
                context,
                pushMessage.id,
                Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notificationChannel = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_PROMO_ID,
            NotificationManager.IMPORTANCE_HIGH
        ).setName(Constants.NOTIFICATION_CHANNEL_PROMO_NAME)
            .setShowBadge(true)
            .setVibrationEnabled(true)
            .setImportance(NotificationManager.IMPORTANCE_HIGH)
            .setVibrationPattern(longArrayOf(0, 500))
            .setLightsEnabled(true)
            .setLightColor(Color.GREEN)
            .build()
        notificationManagerCompat.createNotificationChannel(notificationChannel)

        val notification =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_PROMO_ID)
                .apply {
                    setContentTitle(pushMessage.title)
                    pushMessage.subtitle?.let { setSubText(it) }
                    setContentText(pushMessage.description)
                    setSmallIcon(iconPackR.drawable.ic_stat_notify)
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setAutoCancel(true)
                    setSilent(false)
                    pendingIntent?.let {
                        setContentIntent(it)
                    }
                    setColor(
                        ContextCompat.getColor(
                            context,
                            R.color.red_600_red_300
                        )
                    )
                }.build()

        // Generate a unique ID using the current timestamp
        val notificationId = (System.currentTimeMillis()).toInt()
        notificationManagerCompat.notify(notificationId, notification)
    }
}