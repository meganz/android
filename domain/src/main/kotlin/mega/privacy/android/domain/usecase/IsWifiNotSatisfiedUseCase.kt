package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import javax.inject.Inject

/**
 * Use Case that checks whether the user is connected to Wi-Fi or not, when Camera Uploads is
 * configured to only run in Wi-Fi.
 */
class IsWifiNotSatisfiedUseCase @Inject constructor(
    private val isCameraUploadsByWifiUseCase: IsCameraUploadsByWifiUseCase,
    private val networkRepository: NetworkRepository,
) {
    /**
     * Invocation function
     *
     * @return true if the user only selects the Wi-Fi option to run Camera Uploads, but is not
     * connected to Wi-Fi
     */
    suspend operator fun invoke(): Boolean =
        isCameraUploadsByWifiUseCase() && !networkRepository.isOnWifi()
}
