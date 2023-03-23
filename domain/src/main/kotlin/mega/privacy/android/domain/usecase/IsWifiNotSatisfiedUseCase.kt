package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Check if camera upload is by wifi only, but not using wifi
 *
 * @return true, if wifi constraint is not satisfied
 */
class IsWifiNotSatisfiedUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke(): Boolean =
        cameraUploadRepository.isSyncByWifi() && !networkRepository.isOnWifi()
}
