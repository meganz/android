package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import timber.log.Timber

/**
 * Worker to send regular camera upload sync heart beats
 */
class SyncHeartbeatCameraUploadWorker(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("SyncHeartbeatCameraUploadWorker: doWork()")
        return try {
            CameraUploadSyncManager.doRegularHeartbeat()
            Result.success()
        } catch (throwable: Throwable) {
            Timber.d("SyncHeartbeatCameraUploadWorker: doWork() fail")
            Result.failure()
        }
    }
}
