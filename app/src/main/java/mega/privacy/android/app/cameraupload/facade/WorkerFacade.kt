package mega.privacy.android.app.cameraupload.facade

import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
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
    private val workManager: WorkManager,
) : WorkerGateway {

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of CameraUploadsService
     *
     * @return The result of the job
     */
    override suspend fun fireCameraUploadJob() {
        // Check if CU periodic worker is working. If yes, then don't start a single one
        if (!checkWorkerRunning(CAMERA_UPLOAD_TAG)) {
            Timber.d("No CU periodic process currently running, proceed with one time request")
            val cameraUploadWorkRequest = OneTimeWorkRequest.Builder(
                StartCameraUploadWorker::class.java
            )
                .addTag(SINGLE_CAMERA_UPLOAD_TAG)
                .build()

            workManager
                .enqueueUniqueWork(
                    SINGLE_CAMERA_UPLOAD_TAG,
                    ExistingWorkPolicy.KEEP,
                    cameraUploadWorkRequest
                )
            Timber.d(
                "CameraUpload Single Job Work Status: ${
                    workManager.getWorkInfosByTag(SINGLE_CAMERA_UPLOAD_TAG)
                }"
            )
            // If no CU periodic worker are currently running, cancel the worker
            // It will be rescheduled at the end of the one time request
            cancelPeriodicCameraUploadWorkRequest()
            Timber.d("fireCameraUploadJob() SUCCESS")
        } else {
            Timber.d("CU periodic process currently running, cannot proceed with one time request")
            Timber.d("fireCameraUploadJob() FAIL")
        }
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
        workManager
            .enqueueUniqueWork(
                STOP_CAMERA_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                stopUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Stop Job Work Status: ${
                workManager.getWorkInfosByTag(STOP_CAMERA_UPLOAD_TAG)
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
        workManager
            .enqueueUniquePeriodicWork(
                CAMERA_UPLOAD_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cameraUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Schedule Work Status: ${
                workManager.getWorkInfosByTag(CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("scheduleCameraUploadJob() SUCCESS")
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
        workManager
            .enqueueUniquePeriodicWork(
                HEART_BEAT_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cuSyncActiveHeartbeatWorkRequest
            )
        Timber.d(
            "CameraUpload Schedule Heartbeat Work Status: ${
                workManager.getWorkInfosByTag(HEART_BEAT_TAG)
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
     * Cancel all camera upload workers.
     * Cancel all camera upload sync heartbeat workers.
     */
    override suspend fun cancelCameraUploadAndHeartbeatWorkRequest() {
        listOf(
            CAMERA_UPLOAD_TAG,
            SINGLE_CAMERA_UPLOAD_TAG,
            HEART_BEAT_TAG,
            SINGLE_HEART_BEAT_TAG
        ).forEach {
            workManager.cancelAllWorkByTag(it)
        }
        Timber.d("cancelCameraUploadAndHeartbeatWorkRequest() SUCCESS")
    }

    /**
     * Cancel the Camera Upload periodic worker
     */
    private fun cancelPeriodicCameraUploadWorkRequest() {
        workManager
            .cancelAllWorkByTag(CAMERA_UPLOAD_TAG)
        Timber.d("cancelPeriodicCameraUploadWorkRequest() SUCCESS")
    }

    /**
     * Check if a worker is currently running given his tag
     *
     * @param tag
     */
    private suspend fun checkWorkerRunning(tag: String): Boolean {
        return workManager.getWorkInfosByTag(tag).await()
            ?.map { workInfo -> workInfo.state == WorkInfo.State.RUNNING }
            ?.contains(true)
            ?: false
    }

}
