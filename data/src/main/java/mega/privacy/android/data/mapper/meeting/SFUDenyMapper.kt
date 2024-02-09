package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.SFUDenyType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert SFU deny to [SFUDenyType]
 */
internal class SFUDenyMapper @Inject constructor() {
    operator fun invoke(type: Int): SFUDenyType = when (type) {
        MegaChatCall.SFU_DENY_JOIN -> SFUDenyType.Join
        MegaChatCall.SFU_DENY_AUDIO -> SFUDenyType.Audio
        MegaChatCall.SFU_DENY_INVALID -> SFUDenyType.Invalid
        else -> SFUDenyType.Unknown
    }
}