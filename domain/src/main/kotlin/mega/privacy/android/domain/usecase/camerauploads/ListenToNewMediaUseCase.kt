package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that listens to new Photos and Videos captured by the Device
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class ListenToNewMediaUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param forceEnqueue True if the worker should be enqueued even if it is already running
     *                     Used for enqueueing the same worker from itself
     */
    suspend operator fun invoke(forceEnqueue: Boolean) =
        cameraUploadsRepository.listenToNewMedia(forceEnqueue)
}
