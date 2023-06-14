package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use case for determine whether device is charging or not
 */
class IsChargingUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {


    /**
     * invoke
     * @return [Boolean] whether device is charging or not
     */
    suspend operator fun invoke() = cameraUploadRepository.isCharging()
}
