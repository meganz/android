package mega.privacy.android.data.gateway

import androidx.work.CoroutineWorker

/**
 * Provide the different coroutine workers class
 */
interface WorkerClassGateway {

    /**
     * Provide the camera uploads worker class
     */
    val cameraUploadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the downloads worker class
     */
    val downloadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the chat uploads worker class
     */
    val chatUploadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the sync heartbeat camera uploads worker class
     */
    val syncHeartbeatCameraUploadWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the delete oldest completed transfer worker class
     */
    val deleteOldestCompletedTransferWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the new media worker class
     */
    val newMediaWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the uploads worker class
     */
    val uploadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the offline sync worker class
     */
    val offlineSyncWorkerClass: Class<out CoroutineWorker>
}
