package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import javax.inject.Inject

/**
 * Mapper to convert [OccurrenceFrequencyType] to [DropdownOccurrenceType]
 */
class OccurrenceFrequencyTypeMapper @Inject constructor() {
    operator fun invoke(type: OccurrenceFrequencyType): DropdownOccurrenceType? =
        when (type) {
            OccurrenceFrequencyType.Daily -> DropdownOccurrenceType.Day
            OccurrenceFrequencyType.Weekly -> DropdownOccurrenceType.Week
            OccurrenceFrequencyType.Monthly -> DropdownOccurrenceType.Month
            else -> null
        }
}