package mega.privacy.android.data.facade

import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_FLEX_MILLIS
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.WorkerClassGateway
import mega.privacy.android.data.worker.ChatUploadsWorker
import mega.privacy.android.data.worker.DeleteOldestCompletedTransfersWorker
import mega.privacy.android.data.worker.DownloadsWorker
import mega.privacy.android.data.worker.NewMediaWorker
import mega.privacy.android.data.worker.OfflineSyncWorker.Companion.OFFLINE_SYNC_WORKER_TAG
import mega.privacy.android.data.worker.UploadsWorker
import mega.privacy.android.domain.monitoring.CrashReporter
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Tag identifying the Camera Uploads periodic worker when enqueued
 */
private const val CAMERA_UPLOAD_TAG = "CAMERA_UPLOAD_TAG"

/**
 * Tag identifying the Camera Uploads one-time worker when enqueued
 */
private const val SINGLE_CAMERA_UPLOAD_TAG = "MEGA_SINGLE_CAMERA_UPLOAD_TAG"

/**
 * Tag identifying the Heartbeat periodic worker when enqueued
 */
private const val HEART_BEAT_TAG = "HEART_BEAT_TAG"

/**
 * Interval to run the Heartbeat periodic worker worker
 */
private val UP_TO_DATE_HEARTBEAT_INTERVAL = 30.minutes

/**
 * Interval to run the Camera Uploads periodic worker
 */
private val CU_SCHEDULER_INTERVAL = 60.minutes

/**
 * Responsible of managing the queue of workers in the WorkManager
 */
class WorkManagerGatewayImpl @Inject constructor(
    private val workManager: WorkManager,
    private val crashReporter: CrashReporter,
    private val workerClassGateway: WorkerClassGateway,
) : WorkManagerGateway {

    override suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest() {
        workManager.debugWorkInfo(crashReporter)

        val workRequest =
            OneTimeWorkRequest.Builder(workerClassGateway.deleteOldestCompletedTransferWorkerClass)
                .addTag(DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG)
                .build()

        workManager
            .enqueueUniqueWork(
                DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    override suspend fun enqueueDownloadsWorkerRequest() {
        workManager.debugWorkInfo(crashReporter)

        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.downloadsWorkerClass)
                .addTag(DownloadsWorker.SINGLE_DOWNLOAD_TAG)
                .build()
        workManager
            .enqueueUniqueWork(
                DownloadsWorker.SINGLE_DOWNLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    override suspend fun enqueueChatUploadsWorkerRequest() {
        workManager.debugWorkInfo(crashReporter)

        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.chatUploadsWorkerClass)
                .addTag(ChatUploadsWorker.SINGLE_CHAT_UPLOAD_TAG)
                .build()
        workManager
            .enqueueUniqueWork(
                ChatUploadsWorker.SINGLE_CHAT_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    override suspend fun enqueueNewMediaWorkerRequest(forceEnqueue: Boolean) {
        workManager.debugWorkInfo(crashReporter)

        val tag = NewMediaWorker.NEW_MEDIA_WORKER_TAG
        if (forceEnqueue || !(isWorkerEnqueuedOrRunning(tag))) {
            val workRequest =
                OneTimeWorkRequest.Builder(workerClassGateway.newMediaWorkerClass)
                    .setConstraints(
                        Constraints.Builder()
                            .addContentUriTrigger(
                                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                                true
                            )
                            .addContentUriTrigger(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                true
                            )
                            .addContentUriTrigger(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                true
                            )
                            .addContentUriTrigger(
                                MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                                true
                            )
                            .build()
                    )
                    .addTag(NewMediaWorker.NEW_MEDIA_WORKER_TAG)
                    .build()
            workManager.enqueue(workRequest)
        } else {
            Timber.d("New media worker is already running, cannot proceed with one time request")
        }
    }

    override suspend fun startCameraUploads() {
        // Check if CU periodic worker is working. If yes, then don't start a single one
        if (!isWorkerRunning(CAMERA_UPLOAD_TAG)) {
            workManager.debugWorkInfo(crashReporter)

            Timber.d("No CU periodic process currently running, proceed with one time request")
            val cameraUploadWorkRequest =
                OneTimeWorkRequest.Builder(workerClassGateway.cameraUploadsWorkerClass)
                    .addTag(SINGLE_CAMERA_UPLOAD_TAG)
                    .setBackoffCriteria(
                        backoffPolicy = BackoffPolicy.LINEAR,
                        duration = MIN_BACKOFF_MILLIS.milliseconds.toJavaDuration(),
                    )
                    .build()

            workManager
                .enqueueUniqueWork(
                    SINGLE_CAMERA_UPLOAD_TAG,
                    ExistingWorkPolicy.KEEP,
                    cameraUploadWorkRequest,
                ).await()

            Timber.d("CameraUploads Unique Work enqueued")
            // If no CU periodic worker are currently running, cancel the worker
            // It will be rescheduled at the end of the one time request
            cancelPeriodicCameraUploadWorkRequest()
        } else {
            Timber.d("CU periodic process currently running, cannot proceed with one time request")
        }
    }

    override suspend fun stopCameraUploads() {
        cancelOneTimeCameraUploadWorkRequest()
        cancelPeriodicCameraUploadWorkRequest()
    }

    override suspend fun scheduleCameraUploads() {
        scheduleCameraUploadSyncActiveHeartbeat()

        workManager.debugWorkInfo(crashReporter)

        // periodic work that runs during the last 5 minutes of every one hour period
        val cameraUploadWorkRequest = PeriodicWorkRequest.Builder(
            workerClass = workerClassGateway.cameraUploadsWorkerClass,
            repeatInterval = CU_SCHEDULER_INTERVAL.toJavaDuration(),
            flexInterval = MIN_PERIODIC_FLEX_MILLIS.milliseconds.toJavaDuration(),
        )
            .addTag(CAMERA_UPLOAD_TAG)
            .setInitialDelay(duration = CU_SCHEDULER_INTERVAL.toJavaDuration())
            .build()
        workManager
            .enqueueUniquePeriodicWork(
                CAMERA_UPLOAD_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cameraUploadWorkRequest
            ).await()
        Timber.d("CameraUploads Periodic Work enqueued")
    }

    /**
     * Schedule camera uploads active heartbeat worker
     */
    private suspend fun scheduleCameraUploadSyncActiveHeartbeat() {
        workManager.debugWorkInfo(crashReporter)

        // periodic work that runs during the last 5 minutes of every half an hour period
        val cuSyncActiveHeartbeatWorkRequest = PeriodicWorkRequest.Builder(
            workerClass = workerClassGateway.syncHeartbeatCameraUploadWorkerClass,
            repeatInterval = UP_TO_DATE_HEARTBEAT_INTERVAL.toJavaDuration(),
            flexInterval = MIN_PERIODIC_FLEX_MILLIS.milliseconds.toJavaDuration(),
        )
            .addTag(HEART_BEAT_TAG)
            .build()
        workManager
            .enqueueUniquePeriodicWork(
                HEART_BEAT_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cuSyncActiveHeartbeatWorkRequest
            ).await()
        Timber.d("CameraUploads Heartbeat Periodic Work enqueued")
    }

    /**
     * Cancel all camera upload workers.
     * Cancel all camera upload sync heartbeat workers.
     * Cancel new media worker.
     */
    override suspend fun cancelCameraUploadsAndHeartbeatWorkRequest() {
        listOf(
            CAMERA_UPLOAD_TAG,
            SINGLE_CAMERA_UPLOAD_TAG,
            HEART_BEAT_TAG,
            NewMediaWorker.NEW_MEDIA_WORKER_TAG,
        ).forEach {
            workManager.cancelAllWorkByTag(it).await()
        }
        Timber.d("cancelCameraUploadAndHeartbeatWorkRequest() SUCCESS")
    }

    /**
     * Cancel the Camera Upload one-time worker
     */
    private suspend fun cancelOneTimeCameraUploadWorkRequest() {
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
    private fun isWorkerRunning(tag: String): Boolean {
        return workManager.getWorkInfosByTag(tag).get()
            ?.map { workInfo -> workInfo.state == WorkInfo.State.RUNNING }
            ?.contains(true)
            ?: false
    }

    /**
     * Check if a worker is currently enqueued or running given his tag
     *
     * @param tag
     */
    private fun isWorkerEnqueuedOrRunning(tag: String): Boolean {
        return workManager.getWorkInfosByTag(tag).get()
            ?.map { workInfo -> workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING }
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

    override fun monitorDownloadsStatusInfo() =
        workManager.getWorkInfosByTagFlow(DownloadsWorker.SINGLE_DOWNLOAD_TAG)

    override fun monitorChatUploadsStatusInfo() =
        workManager.getWorkInfosByTagFlow(ChatUploadsWorker.SINGLE_CHAT_UPLOAD_TAG)

    override suspend fun enqueueUploadsWorkerRequest() {
        workManager.debugWorkInfo(crashReporter)

        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.uploadsWorkerClass)
                .addTag(UploadsWorker.SINGLE_UPLOAD_TAG)
                .build()
        workManager
            .enqueueUniqueWork(
                UploadsWorker.SINGLE_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    override fun monitorUploadsStatusInfo() =
        workManager.getWorkInfosByTagFlow(UploadsWorker.SINGLE_UPLOAD_TAG)

    override suspend fun startOfflineSync() {
        workManager.debugWorkInfo(crashReporter)
        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.offlineSyncWorkerClass)
                .addTag(OFFLINE_SYNC_WORKER_TAG)
                .build()
        workManager
            .enqueueUniqueWork(
                OFFLINE_SYNC_WORKER_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }
}

/**
 * Prints the list of pending [WorkInfo] in the log.
 */
suspend fun WorkManager.debugWorkInfo(crashReporter: CrashReporter) {
    getWorkInfosFlow(
        WorkQuery.fromStates(
            WorkInfo.State.ENQUEUED,
        )
    ).firstOrNull()
        ?.map { it.tags }
        ?.groupBy { it }
        ?.mapValues { it.value.size }
        ?.let {
            Timber.d("Worker pending list: $it")
            crashReporter.log("Worker pending list: $it")
        }
        ?: Timber.d("Worker pending list: empty")
}
