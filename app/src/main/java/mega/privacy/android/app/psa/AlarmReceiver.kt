package mega.privacy.android.app.psa

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.TimeUtils.SECOND
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class AlarmReceiver : BroadcastReceiver() {
    private val megaApi by lazy { MegaApplication.getInstance().megaApi }

    override fun onReceive(context: Context, intent: Intent) {
        wakeLock.acquire(30 * SECOND)  // The wakelock will be held for at most 30 Secs

        megaApi.getPSAWithUrl(object : BaseListener(context) {

            override fun onRequestFinish(
                api: MegaApiJava,
                request: MegaRequest,
                e: MegaError
            ) {
                super.onRequestFinish(api, request, e)

                // API response may arrive after stopChecking, in this case we shouldn't
                // emit PSA anymore.
                if (e.errorCode == MegaError.API_OK) {
                    callback?.invoke(
                        Psa(
                            request.number.toInt(), request.name, request.text, request.file,
                            request.password, request.link, request.email
                        )
                    )
                }

                setAlarm(context, PsaManager.GET_PSA_INTERVAL_MS, callback)
            }
        })
    }

    companion object {
        private const val CHECK_PSA_INTENT = "android.intent.action.checking.psa"

        /** The wake lock being held while getting the PSA with the Url */
        val wakeLock: PowerManager.WakeLock by lazy {
            (MegaApplication.getInstance()
                .getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mega::PsaWakelockTag")
            }
        }


        /** the callback being called when getting the PSA with the Url finished */
        var callback: ((Psa) -> Unit)? = null

        /**
         * Cancel the alarm of timed checking the PSA
         * @param context the Context
         */
        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            /* cancel any pending alarm */
            alarmManager.cancel(pendingIntent)
        }

        /**
         * Start the alarm of timed checking the PSA
         * @param context the Context
         * @param delayMs the delay of firing the alarm since now
         * @param cb the callback being called when getting the PSA with the Url finished
         */
        fun setAlarm(context: Context, delayMs: Long, cb: ((Psa) -> Unit)? = null) {
            cancelAlarm(context)

            callback = cb
            val time = System.currentTimeMillis() + delayMs
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            /* fire the broadcast */
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
        }

        /** The pending intent to send a broadcast when firing the alarm */
        private val pendingIntent: PendingIntent
            get() {
                val context = MegaApplication.getInstance().applicationContext
                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                alarmIntent.action = CHECK_PSA_INTENT

                return PendingIntent.getBroadcast(
                    context,
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
    }
}