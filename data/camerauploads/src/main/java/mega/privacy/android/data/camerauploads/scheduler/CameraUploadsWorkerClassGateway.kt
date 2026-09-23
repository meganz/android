package mega.privacy.android.data.camerauploads.scheduler

import androidx.work.CoroutineWorker
import mega.privacy.android.data.camerauploads.worker.CameraUploadsWorker
import mega.privacy.android.data.camerauploads.worker.NewMediaWorker
import mega.privacy.android.data.camerauploads.worker.SyncHeartbeatCameraUploadWorker
import javax.inject.Inject

/**
 * Provides the classes of the camera uploads workers, so tests can schedule a stand-in.
 */
internal interface CameraUploadsWorkerClassGateway {

    /**
     * Provide the camera uploads worker class
     */
    val cameraUploadsWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the new media worker class
     */
    val newMediaWorkerClass: Class<out CoroutineWorker>

    /**
     * Provide the sync heartbeat camera uploads worker class
     */
    val syncHeartbeatCameraUploadWorkerClass: Class<out CoroutineWorker>
}

/**
 * Implementation of [CameraUploadsWorkerClassGateway]
 */
internal class CameraUploadsWorkerClassGatewayImpl @Inject constructor() :
    CameraUploadsWorkerClassGateway {

    override val cameraUploadsWorkerClass = CameraUploadsWorker::class.java

    override val newMediaWorkerClass = NewMediaWorker::class.java

    override val syncHeartbeatCameraUploadWorkerClass = SyncHeartbeatCameraUploadWorker::class.java
}
