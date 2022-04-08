package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.sync.cusync.CuSyncManager
import mega.privacy.android.app.utils.LogUtil.logDebug

/**
 * Worker to send regular camera upload sync heart beats
 */
class SyncHeartbeatWorker(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        logDebug("SyncHeartbeatCameraUploadWork: doWork()")
        return try {
            CuSyncManager.doRegularHeartbeat()
            Result.success()
        } catch (throwable: Throwable) {
            logDebug("SyncHeartbeatCameraUploadWork: doWork() fail")
            Result.failure()
        }
    }
}
