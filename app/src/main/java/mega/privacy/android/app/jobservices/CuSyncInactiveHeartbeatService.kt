package mega.privacy.android.app.jobservices

import android.app.job.JobParameters
import android.app.job.JobService
import mega.privacy.android.app.sync.cusync.CuSyncManager

class CuSyncInactiveHeartbeatService: JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        if (CuSyncManager.isActive()) {
            return false
        }

        CuSyncManager.doInactiveHeartbeat {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}
