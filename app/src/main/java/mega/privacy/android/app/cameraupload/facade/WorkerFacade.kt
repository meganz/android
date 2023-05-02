package mega.privacy.android.app.cameraupload.facade

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import mega.privacy.android.data.gateway.WorkerGateway
import mega.privacy.android.data.worker.EXTRA_ABORTED
import mega.privacy.android.data.worker.StartCameraUploadWorker
import mega.privacy.android.data.worker.StopCameraUploadWorker
import mega.privacy.android.data.worker.SyncHeartbeatCameraUploadWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Worker tags
 */
private const val CAMERA_UPLOAD_TAG = "CAMERA_UPLOAD_TAG"
private const val SINGLE_CAMERA_UPLOAD_TAG = "MEGA_SINGLE_CAMERA_UPLOAD_TAG"
private const val HEART_BEAT_TAG = "HEART_BEAT_TAG"
private const val SINGLE_HEART_BEAT_TAG = "SINGLE_HEART_BEAT_TAG"
private const val STOP_CAMERA_UPLOAD_TAG = "STOP_CAMERA_UPLOAD_TAG"

/**
 * Job time periods
 */
private const val UP_TO_DATE_HEARTBEAT_INTERVAL: Long = 30 // Minutes
private const val HEARTBEAT_FLEX_INTERVAL: Long = 20 // Minutes
private const val CU_SCHEDULER_INTERVAL: Long = 1 // Hour
private const val SCHEDULER_FLEX_INTERVAL: Long = 50 // Minutes
private const val CU_RESCHEDULE_INTERVAL: Long = 5000 // Milliseconds

/**
 * Worker Facade implements [WorkerGateway]
 *
 * To be moved to data layer once Worker is moved to data layer
 */
class WorkerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkerGateway {

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of CameraUploadsService
     *
     * @return The result of the job
     */
    override suspend fun fireCameraUploadJob() {
        val cameraUploadWorkRequest = OneTimeWorkRequest.Builder(
            StartCameraUploadWorker::class.java
        )
            .addTag(SINGLE_CAMERA_UPLOAD_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SINGLE_CAMERA_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                cameraUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Single Job Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(SINGLE_CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("fireCameraUploadJob() SUCCESS")
    }

    /**
     * Fire a request to stop camera upload service.
     *
     * @param aborted true if the Camera Uploads has been aborted prematurely
     */
    override suspend fun fireStopCameraUploadJob(aborted: Boolean) {
        val stopUploadWorkRequest = OneTimeWorkRequest.Builder(
            StopCameraUploadWorker::class.java
        )
            .addTag(STOP_CAMERA_UPLOAD_TAG)
            .setInputData(
                Data.Builder()
                    .putBoolean(EXTRA_ABORTED, aborted)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                STOP_CAMERA_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                stopUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Stop Job Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(STOP_CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("fireStopCameraUploadJob() SUCCESS")
    }

    /**
     * Schedule job of camera upload
     *
     * @return The result of schedule job
     */
    override suspend fun scheduleCameraUploadJob() {
        scheduleCameraUploadSyncActiveHeartbeat()
        // periodic work that runs during the last 10 minutes of every one hour period
        val cameraUploadWorkRequest = PeriodicWorkRequest.Builder(
            StartCameraUploadWorker::class.java,
            CU_SCHEDULER_INTERVAL,
            TimeUnit.HOURS,
            SCHEDULER_FLEX_INTERVAL,
            TimeUnit.MINUTES
        )
            .addTag(CAMERA_UPLOAD_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                CAMERA_UPLOAD_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cameraUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Schedule Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("scheduleCameraUploadJob() SUCCESS")
    }

    /**
     * Restart Camera Uploads by executing [StopCameraUploadWorker] and [StartCameraUploadWorker]
     * sequentially through Work Chaining
     *
     */
    override suspend fun fireRestartCameraUploadJob() {
        val stopCameraUploadRequest = OneTimeWorkRequest.Builder(
            StopCameraUploadWorker::class.java
        )
            .addTag(STOP_CAMERA_UPLOAD_TAG)
            .build()
        val startCameraUploadRequest = OneTimeWorkRequest.Builder(
            StartCameraUploadWorker::class.java
        )
            .addTag(SINGLE_CAMERA_UPLOAD_TAG)
            .build()
        WorkManager.getInstance(context)
            .beginWith(stopCameraUploadRequest)
            .then(startCameraUploadRequest)
            .enqueue()
    }

    /**
     * Schedule job of camera upload active heartbeat
     *
     */
    private fun scheduleCameraUploadSyncActiveHeartbeat() {
        // periodic work that runs during the last 10 minutes of every half an hour period
        val cuSyncActiveHeartbeatWorkRequest = PeriodicWorkRequest.Builder(
            SyncHeartbeatCameraUploadWorker::class.java,
            UP_TO_DATE_HEARTBEAT_INTERVAL,
            TimeUnit.MINUTES,
            HEARTBEAT_FLEX_INTERVAL,
            TimeUnit.MINUTES
        )
            .addTag(HEART_BEAT_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                HEART_BEAT_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cuSyncActiveHeartbeatWorkRequest
            )
        Timber.d(
            "CameraUpload Schedule Heartbeat Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(HEART_BEAT_TAG)
            }"
        )
        Timber.d("scheduleCameraUploadSyncActiveHeartbeat() SUCCESS")
    }

    /**
     * Reschedule Camera Upload with time interval
     */
    override suspend fun rescheduleCameraUpload() {
        fireStopCameraUploadJob()
        delay(CU_RESCHEDULE_INTERVAL)
        scheduleCameraUploadJob()
    }

    /**
     * Stop the camera upload work by tag.
     * Stop regular camera upload sync heartbeat work by tag.
     *
     */
    override suspend fun stopCameraUploadSyncHeartbeatWorkers() {
        val manager = WorkManager.getInstance(context)
        listOf(
            CAMERA_UPLOAD_TAG,
            SINGLE_CAMERA_UPLOAD_TAG,
            HEART_BEAT_TAG,
            SINGLE_HEART_BEAT_TAG
        ).forEach {
            manager.cancelAllWorkByTag(it)
        }
        Timber.d("stopCameraUploadSyncHeartbeatWorkers() SUCCESS")
    }
}
