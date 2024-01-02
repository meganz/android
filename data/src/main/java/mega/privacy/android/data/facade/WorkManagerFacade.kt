package mega.privacy.android.data.facade

import androidx.work.BackoffPolicy
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
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.worker.CameraUploadsWorker
import mega.privacy.android.data.worker.DeleteOldestCompletedTransfersWorker
import mega.privacy.android.data.worker.DownloadsWorker
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

/**
 * Job time periods
 */
private const val UP_TO_DATE_HEARTBEAT_INTERVAL: Long = 30 // Minutes
private const val HEARTBEAT_FLEX_INTERVAL: Long = 20 // Minutes
private const val CU_SCHEDULER_INTERVAL: Long = 60 // Minutes
private const val SCHEDULER_FLEX_INTERVAL: Long = 50 // Minutes

internal class WorkManagerFacade @Inject constructor(
    private val workManager: WorkManager,
) : WorkManagerGateway {

    override suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest() {
        val workRequest =
            OneTimeWorkRequest.Builder(DeleteOldestCompletedTransfersWorker::class.java)
                .addTag(DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG)
                .build()

        workManager
            .enqueueUniqueWork(
                DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    override fun enqueueDownloadsWorkerRequest() {
        val request = OneTimeWorkRequest.Builder(DownloadsWorker::class.java)
            .addTag(DownloadsWorker.SINGLE_DOWNLOAD_TAG)
            .build()
        workManager
            .enqueueUniqueWork(
                DownloadsWorker.SINGLE_DOWNLOAD_TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    override suspend fun startCameraUploads() {
        // Check if CU periodic worker is working. If yes, then don't start a single one
        if (!checkWorkerRunning(CAMERA_UPLOAD_TAG)) {
            Timber.d("No CU periodic process currently running, proceed with one time request")
            val cameraUploadWorkRequest = OneTimeWorkRequest.Builder(
                CameraUploadsWorker::class.java
            )
                .addTag(SINGLE_CAMERA_UPLOAD_TAG)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10,
                    TimeUnit.SECONDS
                )
                .build()

            workManager
                .enqueueUniqueWork(
                    SINGLE_CAMERA_UPLOAD_TAG,
                    ExistingWorkPolicy.KEEP,
                    cameraUploadWorkRequest
                ).await()
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

    override suspend fun stopCameraUploads() {
        cancelUniqueCameraUploadWorkRequest()
        cancelPeriodicCameraUploadWorkRequest()

        Timber.d("fireStopCameraUploadJob() SUCCESS")
    }

    override suspend fun scheduleCameraUploads() {
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
            ).await()
        Timber.d(
            "CameraUpload Schedule Work Status: ${
                workManager.getWorkInfosByTag(CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("scheduleCameraUploadJob() SUCCESS")
    }

    /**
     * Schedule camera uploads active heartbeat worker
     */
    private suspend fun scheduleCameraUploadSyncActiveHeartbeat() {
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
            ).await()
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
            workManager.cancelAllWorkByTag(it).await()
        }
        Timber.d("cancelCameraUploadAndHeartbeatWorkRequest() SUCCESS")
    }

    /**
     * Cancel the Camera Upload unique worker
     */
    private suspend fun cancelUniqueCameraUploadWorkRequest() {
        workManager
            .cancelAllWorkByTag(SINGLE_CAMERA_UPLOAD_TAG)
            .await()
        Timber.d("cancelUniqueCameraUploadWorkRequest() SUCCESS")
    }

    /**
     * Cancel the Camera Upload periodic worker
     */
    private suspend fun cancelPeriodicCameraUploadWorkRequest() {
        workManager
            .cancelAllWorkByTag(CAMERA_UPLOAD_TAG)
            .await()
        Timber.d("cancelPeriodicCameraUploadWorkRequest() SUCCESS")
    }

    /**
     * Check if a worker is currently running given his tag
     *
     * @param tag
     */
    private fun checkWorkerRunning(tag: String): Boolean {
        return workManager.getWorkInfosByTag(tag).get()
            ?.map { workInfo -> workInfo.state == WorkInfo.State.RUNNING }
            ?.contains(true)
            ?: false
    }

    override fun monitorCameraUploadsStatusInfo(): Flow<List<WorkInfo>> {
        val uploadFlow = workManager.getWorkInfosByTagFlow(CAMERA_UPLOAD_TAG)
        val singleUploadFlow =
            workManager.getWorkInfosByTagFlow(SINGLE_CAMERA_UPLOAD_TAG)
        return merge(uploadFlow, singleUploadFlow).mapNotNull {
            it.takeUnless { it.isEmpty() }
        }
    }
}
