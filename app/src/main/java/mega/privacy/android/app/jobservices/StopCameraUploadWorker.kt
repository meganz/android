package mega.privacy.android.app.jobservices

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.utils.LogUtil.logDebug

/**
 * Worker to stop upload images task
 */
class StopCameraUploadWorker(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        logDebug("StopCameraUploadWorker: doWork()")
        return try {
            if (CameraUploadsService.isServiceRunning) {
                logDebug("StopCameraUploadWorker: Sending stop action")
                val stopIntent = Intent(appContext, CameraUploadsService::class.java)
                stopIntent.action = CameraUploadsService.ACTION_STOP
                ContextCompat.startForegroundService(appContext, stopIntent)
            }
            Result.success()
        } catch (throwable: Throwable) {
            logDebug("StopCameraUploadWorker: doWork() fail")
            Result.failure()
        }
    }
}
