package mega.privacy.android.nocturn.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationManagerCompat
import mega.privacy.android.nocturn.R
import mega.privacy.android.nocturn.receiver.NocturnCopyTagReceiver
import kotlin.random.Random

class NocturnNotificator(private val context: Context) {
    fun setup() {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.getNotificationChannel(CHANNEL_ID) != null) return

            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESCRIPTION
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun alertAnr(tag: String, summaries: List<String>) {
        val id = Random.nextInt()
        val pendingIntent = createCopyTagPendingIntent(id, tag)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nocturn)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText("$NOTIFICATION_SUMMARY ($tag)")
            .apply {
                if (summaries.isNotEmpty()) {
                    setStyle(
                        BigTextStyle()
                            .setBigContentTitle("Stack code: $tag")
                            .bigText(((summaries + "...").joinToString("\n\n")))
                    )
                }
            }
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun createCopyTagPendingIntent(id: Int, tag: String): PendingIntent {
        val intent = Intent(context, NocturnCopyTagReceiver::class.java).apply {
            putExtra("tag", tag)
        }
        return PendingIntent.getBroadcast(context, id, intent, FLAG_IMMUTABLE)
    }

    private companion object {
        const val CHANNEL_ID: String = "nocturn"

        const val CHANNEL_NAME: String = "Nocturn"

        const val CHANNEL_DESCRIPTION: String = "Monitor Application Not Responding (ANR) Event"

        const val NOTIFICATION_TITLE: String = "ANR Detected!"

        const val NOTIFICATION_SUMMARY: String = "Please check below stack trace"
    }
}
