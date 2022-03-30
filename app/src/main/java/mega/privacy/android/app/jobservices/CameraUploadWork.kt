package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.utils.JobUtil.startCameraUploadService
import mega.privacy.android.app.utils.LogUtil

class CameraUploadWork(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    /**
     * Worker for upload images task
     */
    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        LogUtil.logDebug("CameraUploadWork: startCameraUploadService()")
        return try {
            startCameraUploadService(appContext)
            LogUtil.logDebug("CameraUploadWork: startCameraUploadService() SUCCESS")
            Result.success()
        } catch (throwable: Throwable) {
            LogUtil.logDebug("CameraUploadWork: startCameraUploadService() FAILURE")
            Result.failure()
        }
    }
}
