package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import nz.mega.sdk.MegaChatScheduledRules
import javax.inject.Inject

/**
 * Mapper to occurrence frequency type to [OccurrenceFrequencyType]
 */
internal class OccurrenceFrequencyTypeMapper @Inject constructor() {
    operator fun invoke(freq: Int): OccurrenceFrequencyType =
        occurrenceFreq[freq] ?: OccurrenceFrequencyType.Invalid

    companion object {
        internal val occurrenceFreq = mapOf(
            MegaChatScheduledRules.FREQ_DAILY to OccurrenceFrequencyType.Daily,
            MegaChatScheduledRules.FREQ_WEEKLY to OccurrenceFrequencyType.Weekly,
            MegaChatScheduledRules.FREQ_MONTHLY to OccurrenceFrequencyType.Monthly,
            MegaChatScheduledRules.FREQ_INVALID to OccurrenceFrequencyType.Invalid

        )
    }
}