package mega.privacy.android.data.gateway

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

/**
 *  Functions for starting work requests
 */
interface WorkManagerGateway {

    /**
     *  Enqueue unique work request to delete oldest completed transfers from local storage
     */
    suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest()

    /**
     * Enqueue unique work request to start download worker to monitor the download transfers as a foreground service
     */
    suspend fun enqueueDownloadsWorkerRequest()

    /**
     * Enqueue unique work request to start chat uploads worker to monitor the chat upload transfers as a foreground service
     */
    suspend fun enqueueChatUploadsWorkerRequest()

    /**
     * Enqueue unique work request to start new media worker
     *
     * @param forceEnqueue True if the worker should be enqueued even if it is already running
     *                     Used for enqueueing the same worker from itself
     */
    suspend fun enqueueNewMediaWorkerRequest(forceEnqueue: Boolean)

    /**
     * Queue a one time work request of camera upload to upload immediately.
     * The worker will not be queued if a camera uploads worker is already running
     */
    suspend fun startCameraUploads()

    /**
     * Cancel all camera uploads workers
     */
    suspend fun stopCameraUploads()

    /**
     * Schedule the camera uploads worker
     */
    suspend fun scheduleCameraUploads()

    /**
     * Cancel all camera upload workers.
     * Cancel all camera upload sync heartbeat workers.
     */
    suspend fun cancelCameraUploadAndHeartbeatWorkRequest()

    /**
     * Get CameraUploadsWorker Info
     */
    fun monitorCameraUploadsStatusInfo(): Flow<List<WorkInfo>>

    /**
     * Get DownloadsWorker Info
     */
    fun monitorDownloadsStatusInfo(): Flow<List<WorkInfo>>

    /**
     * Get DownloadsWorker Info
     */
    fun monitorChatUploadsStatusInfo(): Flow<List<WorkInfo>>
}
