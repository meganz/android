package mega.privacy.android.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import mega.privacy.android.app.utils.JobUtil
import timber.log.Timber

class BootEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("BootEventReceiver")
        JobUtil.rescheduleCameraUpload(context)
    }
}
