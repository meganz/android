package mega.privacy.android.data.gateway

/**
 * Worker Gateway
 */
interface WorkerGateway {

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of CameraUploadsService
     *
     */
    suspend fun fireCameraUploadJob()

    /**
     * Fire a request to stop camera upload service.
     *
     * @param aborted true if the Camera Uploads has been aborted prematurely
     */
    suspend fun fireStopCameraUploadJob(aborted: Boolean = true)

    /**
     * Schedule job of camera upload
     *
     * @return The result of schedule job
     */
    suspend fun scheduleCameraUploadJob()

    /**
     * Reschedule Camera Upload with time interval
     */
    suspend fun rescheduleCameraUpload()

    /**
     * Cancel all camera upload workers.
     * Cancel all camera upload sync heartbeat workers.
     */
    suspend fun cancelCameraUploadAndHeartbeatWorkRequest()
}
