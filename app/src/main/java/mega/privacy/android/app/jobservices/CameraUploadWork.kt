package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.utils.JobUtil.SHOULD_IGNORE_ATTRIBUTES
import mega.privacy.android.app.utils.JobUtil.startCameraUploadWork
import mega.privacy.android.app.utils.LogUtil

class CameraUploadWork(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    /**
     * Worker for upload images task
     */
    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        LogUtil.logDebug("CameraUploadWork: startCameraUploadService()")
        val ignoreAttributes = inputData.getBoolean(SHOULD_IGNORE_ATTRIBUTES, false)
        return try {
            startCameraUploadWork(appContext, ignoreAttributes)
            LogUtil.logDebug("CameraUploadWork: startCameraUploadService() SUCCESS")
            Result.success()
        } catch (throwable: Throwable) {
            LogUtil.logDebug("CameraUploadWork: startCameraUploadService() FAILURE")
            Result.failure()
        }
    }
}
