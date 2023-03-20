package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimit
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompression
import javax.inject.Inject

/**
 * Default implementation of [IsChargingRequired]
 *
 * @property getVideoCompressionSizeLimit [GetVideoCompressionSizeLimit]
 * @property isChargingRequiredForVideoCompression [IsChargingRequiredForVideoCompression]
 */
class DefaultIsChargingRequired @Inject constructor(
    private val getVideoCompressionSizeLimit: GetVideoCompressionSizeLimit,
    private val isChargingRequiredForVideoCompression: IsChargingRequiredForVideoCompression,
) : IsChargingRequired {

    override suspend fun invoke(queueSize: Long) =
        isChargingRequiredForVideoCompression() && queueSize > getVideoCompressionSizeLimit()
}
