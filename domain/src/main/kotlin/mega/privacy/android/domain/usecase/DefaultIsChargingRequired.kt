package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import javax.inject.Inject

/**
 * Default implementation of [IsChargingRequired]
 *
 * @property getVideoCompressionSizeLimitUseCase [GetVideoCompressionSizeLimitUseCase]
 * @property isChargingRequiredForVideoCompressionUseCase [IsChargingRequiredForVideoCompressionUseCase]
 */
class DefaultIsChargingRequired @Inject constructor(
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
) : IsChargingRequired {

    override suspend fun invoke(queueSize: Long) =
        isChargingRequiredForVideoCompressionUseCase() && queueSize > getVideoCompressionSizeLimitUseCase()
}
