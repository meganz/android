package mega.privacy.android.data.gateway

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

/**
 * Worker Gateway
 */
interface WorkerGateway {

    /**
     * Queue a one time work request of camera upload to upload immediately.
     * The worker will not be queued if a camera uploads worker is already running
     */
    suspend fun startCameraUploads()

    /**
     * Cancel all camera uploads workers
     *
     * @param shouldReschedule true if the Camera Uploads should be rescheduled at a later time
     */
    suspend fun stopCameraUploads(shouldReschedule: Boolean)

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
}
