package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Indicates whether camera uploads is enabled. This use case provides two different ways
 * to retrieve the enablement value:
 * - Use monitorCameraUploadsEnabled if you need to listen to changes reactively.
 * - Otherwise, call invoke().
 */
class IsCameraUploadsEnabledUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    val monitorCameraUploadsEnabled: Flow<Boolean> =
        cameraUploadsRepository.monitorCameraUploadsEnabled.map { it ?: false }

    /**
     * Invoke
     *
     * @return true if camera uploads is enabled
     */
    suspend operator fun invoke() = cameraUploadsRepository.isCameraUploadsEnabled() ?: false
}
