package mega.privacy.android.data.gateway

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

/**
 * Schedules the camera uploads workers.
 *
 * Implemented by the module that owns those workers, so this module can start, stop and observe
 * camera uploads without knowing which classes run them.
 */
interface CameraUploadsWorkScheduler {

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
     * Cancel all camera uploads workers.
     * Cancel all camera uploads sync heartbeat workers.
     */
    suspend fun cancelCameraUploadsAndHeartbeatWorkRequest()

    /**
     * Get CameraUploadsWorker Info
     */
    fun monitorCameraUploadsStatusInfo(): Flow<List<WorkInfo>>
}
