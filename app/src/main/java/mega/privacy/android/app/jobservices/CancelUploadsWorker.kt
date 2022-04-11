package mega.privacy.android.app.jobservices

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.utils.LogUtil.logDebug

/**
 * Worker to cancel all camera upload transfers.
 */
class CancelUploadsWorker(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        logDebug("CancelUploadsWorker: doWork()")
        return try {
            logDebug("CancelUploadsWorker: Sending cancel all action")
            val cancelIntent = Intent(appContext, CameraUploadsService::class.java)
            cancelIntent.action = CameraUploadsService.ACTION_CANCEL_ALL
            ContextCompat.startForegroundService(appContext, cancelIntent)
            Result.success()
        } catch (throwable: Throwable) {
            logDebug("CancelUploadsWorker: doWork() fail")
            Result.failure()
        }
    }
}
