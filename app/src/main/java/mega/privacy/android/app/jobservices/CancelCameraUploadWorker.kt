package mega.privacy.android.app.jobservices

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

/**
 * Worker to cancel all camera upload transfers.
 */
class CancelCameraUploadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("CancelCameraUploadWorker: doWork()")
        return try {
            Timber.d("CancelCameraUploadWorker: Sending cancel all action")
            val cancelIntent = Intent(appContext, CameraUploadsService::class.java)
            cancelIntent.action = CameraUploadsService.ACTION_CANCEL_ALL
            ContextCompat.startForegroundService(appContext, cancelIntent)
            Result.success()
        } catch (throwable: Throwable) {
            Timber.d("CancelCameraUploadWorker: doWork() fail")
            Result.failure()
        }
    }
}
