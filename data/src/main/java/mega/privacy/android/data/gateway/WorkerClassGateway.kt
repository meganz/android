package mega.privacy.android.data.gateway

import androidx.work.CoroutineWorker

/**
 * Provides the classes of the workers this module owns.
 *
 * Only :data's own workers belong here. A module that owns other workers schedules them itself,
 * as [CameraUploadsWorkScheduler] does for camera uploads.
 */
interface WorkerClassGateway {

    /**
     * Provide the downloads worker class
     */
    val downloadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the chat uploads worker class
     */
    val chatUploadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the delete oldest completed transfer worker class
     */
    val deleteOldestCompletedTransferWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the uploads worker class
     */
    val uploadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the offline sync worker class
     */
    val offlineSyncWorkerClass: Class<out CoroutineWorker>
}
