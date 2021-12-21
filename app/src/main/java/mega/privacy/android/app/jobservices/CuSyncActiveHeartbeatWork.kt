package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.sync.cusync.CuSyncManager
import mega.privacy.android.app.utils.LogUtil.logDebug

class CuSyncActiveHeartbeatWork(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        logDebug("CuSyncManager doActiveHeartbeat")
        CuSyncManager.doActiveHeartbeat {
            logDebug("CuSyncActiveHeartbeatWork doActiveHeartbeat return.")
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}