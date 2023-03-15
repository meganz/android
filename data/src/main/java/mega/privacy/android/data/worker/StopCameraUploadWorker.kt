package mega.privacy.android.data.worker

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.data.wrapper.CameraUploadServiceWrapper
import timber.log.Timber

/**
 * Stop the camera upload service
 */
const val ACTION_STOP = "STOP_SYNC"

/**
 * Notify that the camera upload service is aborted prematurely
 */
const val EXTRA_ABORTED = "EXTRA_ABORTED"

/**
 * Worker to stop upload images task
 */
@HiltWorker
class StopCameraUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cameraUploadServiceWrapper: CameraUploadServiceWrapper,
) : Worker(context, workerParams) {

    /**
     * Sending stop action intent to camera upload service
     */
    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("StopCameraUploadWorker: Working")
        val aborted = inputData.getBoolean(EXTRA_ABORTED, false)
        return try {
            val stopIntent = cameraUploadServiceWrapper.newIntent(context)
            stopIntent.action = ACTION_STOP
            stopIntent.putExtra(EXTRA_ABORTED, aborted)
            ContextCompat.startForegroundService(context, stopIntent)
            Timber.d("StopCameraUploadWorker: Finished with Success")
            Result.success()
        } catch (throwable: Throwable) {
            Timber.d("StopCameraUploadWorker: Finished with Failure")
            Result.failure()
        }
    }
}
