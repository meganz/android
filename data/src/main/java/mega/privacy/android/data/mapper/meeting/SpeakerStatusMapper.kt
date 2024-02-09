package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.SpeakerStatusType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert speaker status to [SpeakerStatusType]
 */
internal class SpeakerStatusMapper @Inject constructor() {
    operator fun invoke(limit: Int): SpeakerStatusType = when (limit) {
        MegaChatCall.SPEAKER_STATUS_DISABLED -> SpeakerStatusType.Disabled
        MegaChatCall.SPEAKER_STATUS_PENDING -> SpeakerStatusType.Pending
        MegaChatCall.SPEAKER_STATUS_ACTIVE -> SpeakerStatusType.Active
        else -> SpeakerStatusType.Unknown
    }
}