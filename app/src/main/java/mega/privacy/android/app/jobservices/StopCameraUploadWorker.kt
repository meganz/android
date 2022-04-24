package mega.privacy.android.app.jobservices

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber
import javax.inject.Inject

/**
 * Worker to stop upload images task
 */
class StopCameraUploadWorker @Inject constructor(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val cameraUploadsServiceWrapper: CameraUploadsServiceWrapper
) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("StopCameraUploadWorker: doWork()")
        return try {
            if (cameraUploadsServiceWrapper.isServiceRunning()) {
                Timber.d("StopCameraUploadWorker: Sending stop action")
                val stopIntent = Intent(appContext, CameraUploadsService::class.java)
                stopIntent.action = CameraUploadsService.ACTION_STOP
                ContextCompat.startForegroundService(appContext, stopIntent)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (throwable: Throwable) {
            Timber.d("StopCameraUploadWorker: doWork() fail")
            Result.failure()
        }
    }
}
