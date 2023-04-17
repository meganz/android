package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import nz.mega.sdk.MegaChatScheduledRules
import javax.inject.Inject

/**
 * Mapper to occurrence frequency type to [MegaChatScheduledRules]
 */
internal class MegaOccurrenceFrequencyTypeMapper @Inject constructor() {
    operator fun invoke(freq: OccurrenceFrequencyType): Int = when (freq) {
        OccurrenceFrequencyType.Daily -> MegaChatScheduledRules.FREQ_DAILY
        OccurrenceFrequencyType.Weekly -> MegaChatScheduledRules.FREQ_WEEKLY
        OccurrenceFrequencyType.Monthly -> MegaChatScheduledRules.FREQ_MONTHLY
        OccurrenceFrequencyType.Invalid -> MegaChatScheduledRules.FREQ_INVALID
    }
}