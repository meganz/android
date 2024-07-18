package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.call.CallQualityType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert call quality to [CallQualityType]
 */
internal class CallQualityMapper @Inject constructor() {
    operator fun invoke(quality: Int): CallQualityType = when (quality) {
        MegaChatCall.CALL_QUALITY_HIGH_DEF -> CallQualityType.HighDef
        MegaChatCall.CALL_QUALITY_HIGH_MEDIUM -> CallQualityType.HighMedium
        MegaChatCall.CALL_QUALITY_HIGH_LOW -> CallQualityType.HighLow
        else -> CallQualityType.Unknown
    }
}