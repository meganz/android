package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimit
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import javax.inject.Inject

/**
 * Default implementation of [IsChargingRequired]
 *
 * @property getVideoCompressionSizeLimit [GetVideoCompressionSizeLimit]
 * @property isChargingRequiredForVideoCompressionUseCase [IsChargingRequiredForVideoCompressionUseCase]
 */
class DefaultIsChargingRequired @Inject constructor(
    private val getVideoCompressionSizeLimit: GetVideoCompressionSizeLimit,
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase,
) : IsChargingRequired {

    override suspend fun invoke(queueSize: Long) =
        isChargingRequiredForVideoCompressionUseCase() && queueSize > getVideoCompressionSizeLimit()
}
