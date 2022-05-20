package mega.privacy.android.app.fcm

import android.app.NotificationManager
import android.content.Intent
import mega.privacy.android.app.main.ManagerActivity
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.Service
import android.os.Build
import mega.privacy.android.app.R
import android.os.IBinder
import androidx.core.app.NotificationCompat
import mega.privacy.android.app.utils.Constants

open class KeepAliveService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotification(
            R.drawable.ic_stat_notify,
            getString(R.string.notification_chat_undefined_content)
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    private fun stop() {
        stopForeground(true)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NEW_MESSAGE_NOTIFICATION_ID)

        stopSelf()
    }

    private fun createNotification(smallIcon: Int, title: String?) {
        val intent = Intent(this, ManagerActivity::class.java)
            .setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE)
            .putExtra(Constants.EXTRA_MOVE_TO_CHAT_SECTION, true)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)


        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mBuilder = NotificationCompat.Builder(this, RETRIEVING_MSG_CHANNEL_ID).apply {
            setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent)
                .setContentText(title)
                .setAutoCancel(false)
        }

        val mNotificationManager =
            this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(
                RETRIEVING_MSG_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_FCM_FETCHING_MESSAGE,
                importance
            ).apply {
                //no sound and vibration for this channel.
                enableVibration(false)
                setSound(null, null)
            }

            val channel = mNotificationManager.getNotificationChannel(RETRIEVING_MSG_CHANNEL_ID)

            if (channel == null) {
                mNotificationManager.createNotificationChannel(notificationChannel)
            }
        }

        val notification = mBuilder.build()
        startForeground(NEW_MESSAGE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val RETRIEVING_MSG_CHANNEL_ID = "Retrieving message"
        const val NEW_MESSAGE_NOTIFICATION_ID = 1086
    }
}