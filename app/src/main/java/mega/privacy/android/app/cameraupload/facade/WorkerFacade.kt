package mega.privacy.android.app.cameraupload.facade

import androidx.lifecycle.asFlow
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import mega.privacy.android.app.cameraupload.CameraUploadsWorker
import mega.privacy.android.data.gateway.WorkerGateway
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsStatusInfoMapper
import mega.privacy.android.data.worker.SyncHeartbeatCameraUploadWorker
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
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

/**
 * Job time periods
 */
private const val UP_TO_DATE_HEARTBEAT_INTERVAL: Long = 30 // Minutes
private const val HEARTBEAT_FLEX_INTERVAL: Long = 20 // Minutes
private const val CU_SCHEDULER_INTERVAL: Long = 60 // Minutes
private const val SCHEDULER_FLEX_INTERVAL: Long = 50 // Minutes

/**
 * Worker Facade implements [WorkerGateway]
 *
 * To be moved to data layer once Worker is moved to data layer
 */
class WorkerFacade @Inject constructor(
    private val workManager: WorkManager,
    private val cameraUploadsStatusInfoMapper: CameraUploadsStatusInfoMapper,
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
                CameraUploadsWorker::class.java
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
     * @param shouldReschedule true if the Camera Uploads should be rescheduled at a later time
     */
    override suspend fun stopCameraUploads(shouldReschedule: Boolean) {
        cancelUniqueCameraUploadWorkRequest()
        cancelPeriodicCameraUploadWorkRequest()

        // Reschedule the Camera Upload to launch in a later time
        // This case only happens if :
        // - the user cancel all transfers without disabling the Camera Uploads
        // - the camera uploads folder have been moved to rubbish bin
        if (shouldReschedule) {
            scheduleCameraUploadJob()
        }
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
            CameraUploadsWorker::class.java,
            CU_SCHEDULER_INTERVAL,
            TimeUnit.MINUTES,
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
     * Cancel the Camera Upload unique worker
     */
    private fun cancelUniqueCameraUploadWorkRequest() {
        workManager
            .cancelAllWorkByTag(SINGLE_CAMERA_UPLOAD_TAG)
        Timber.d("cancelUniqueCameraUploadWorkRequest() SUCCESS")
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

    override fun monitorCameraUploadsStatusInfo(): Flow<CameraUploadsStatusInfo> {
        val uploadFlow = workManager.getWorkInfosByTagLiveData(CAMERA_UPLOAD_TAG).asFlow()
        val singleUploadFlow =
            workManager.getWorkInfosByTagLiveData(SINGLE_CAMERA_UPLOAD_TAG).asFlow()
        return merge(uploadFlow, singleUploadFlow).mapNotNull {
            it.takeUnless { it.isEmpty() }?.let { workInfoList ->
                cameraUploadsStatusInfoMapper(workInfoList.first().progress)
            }
        }
    }
}
