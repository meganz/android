package mega.privacy.android.app.jobservices

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker to stop upload images task
 */
@HiltWorker
class StopCameraUploadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
) :
    Worker(appContext, workerParams) {

    /**
     * Sending stop action intent to camera upload service
     */
    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("StopCameraUploadWorker: doWork()")
        val aborted = inputData.getBoolean(CameraUploadsService.EXTRA_ABORTED, false)
        return try {
            Timber.d("StopCameraUploadWorker: Sending stop action")
            val stopIntent = Intent(appContext, CameraUploadsService::class.java)
            stopIntent.action = CameraUploadsService.ACTION_STOP
            stopIntent.putExtra(CameraUploadsService.EXTRA_ABORTED, aborted)
            ContextCompat.startForegroundService(appContext, stopIntent)
            Result.success()
        } catch (throwable: Throwable) {
            Timber.d("StopCameraUploadWorker: doWork() fail")
            Result.failure()
        }
    }
}
