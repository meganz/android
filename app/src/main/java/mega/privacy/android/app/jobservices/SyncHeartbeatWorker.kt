package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.sync.cusync.CuSyncManager
import timber.log.Timber

/**
 * Worker to send regular camera upload sync heart beats
 */
class SyncHeartbeatWorker(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("SyncHeartbeatCameraUploadWork: doWork()")
        return try {
            CuSyncManager.doRegularHeartbeat()
            Result.success()
        } catch (throwable: Throwable) {
            Timber.d("SyncHeartbeatCameraUploadWork: doWork() fail")
            Result.failure()
        }
    }
}
