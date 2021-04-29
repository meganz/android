package mega.privacy.android.app.psa

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class CheckingJobIntentService : JobIntentService() {

    private val megaApi = MegaApplication.getInstance().megaApi

    companion object {
        /* Give the Job a Unique Id */
        private const val JOB_ID = 1000

        private var callback: ((Psa) -> Unit)? = null

        fun enqueueWork(ctx: Context, intent: Intent, cb: ((Psa) -> Unit)? = null) {
            callback = cb

            enqueueWork(
                ctx,
                CheckingJobIntentService::class.java, JOB_ID, intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        /* your code here */
        megaApi.getPSAWithUrl(object : BaseListener(application) {
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
            }
        })

        /* reset the alarm */
        AlarmReceiver.setAlarm(applicationContext, PsaManager.GET_PSA_INTERVAL_MS, callback)
        stopSelf()
    }
}