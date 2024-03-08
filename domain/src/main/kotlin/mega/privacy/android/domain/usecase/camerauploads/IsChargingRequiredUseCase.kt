package mega.privacy.android.domain.usecase.camerauploads

import javax.inject.Inject

/**
 * Check if is charging required to perform video compression during Camera Uploads process
 */
class IsChargingRequiredUseCase @Inject constructor(
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(queueSize: Long) =
        isChargingRequiredForVideoCompressionUseCase() && queueSize > getVideoCompressionSizeLimitUseCase()
}
