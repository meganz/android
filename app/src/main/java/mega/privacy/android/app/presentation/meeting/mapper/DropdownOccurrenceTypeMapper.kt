package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import javax.inject.Inject

/**
 * Mapper to convert [DropdownOccurrenceType] to [OccurrenceFrequencyType]
 */
class DropdownOccurrenceTypeMapper @Inject constructor() {
    operator fun invoke(type: DropdownOccurrenceType): OccurrenceFrequencyType =
        when (type) {
            DropdownOccurrenceType.Day -> OccurrenceFrequencyType.Daily
            DropdownOccurrenceType.Week -> OccurrenceFrequencyType.Weekly
            DropdownOccurrenceType.Month -> OccurrenceFrequencyType.Monthly
        }
}