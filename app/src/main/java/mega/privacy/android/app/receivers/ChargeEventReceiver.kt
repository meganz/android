package mega.privacy.android.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_POWER_CONNECTED
import mega.privacy.android.app.utils.JobUtil
import timber.log.Timber

class ChargeEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(ACTION_POWER_CONNECTED)) {
            Timber.d("ChargeEventReceiver")
            JobUtil.fireCameraUploadJob(context)
        }
    }
}
