package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert network quality changes to [NetworkQualityType]
 */
internal class NetworkQualityMapper @Inject constructor() {
    operator fun invoke(endCallReason: Int): NetworkQualityType = when (endCallReason) {
        MegaChatCall.NETWORK_QUALITY_BAD -> NetworkQualityType.Bad
        MegaChatCall.NETWORK_QUALITY_GOOD -> NetworkQualityType.Good
        else -> NetworkQualityType.Unknown
    }
}