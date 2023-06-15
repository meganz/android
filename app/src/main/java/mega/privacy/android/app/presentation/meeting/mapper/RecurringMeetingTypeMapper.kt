package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurringMeetingType
import javax.inject.Inject

/**
 * Mapper to convert [RecurringMeetingType] to [OccurrenceFrequencyType]
 */
class RecurringMeetingTypeMapper @Inject constructor() {
    operator fun invoke(recurringType: RecurringMeetingType): OccurrenceFrequencyType =
        when (recurringType) {
            RecurringMeetingType.EveryDay -> OccurrenceFrequencyType.Daily
            RecurringMeetingType.EveryWeek -> OccurrenceFrequencyType.Weekly
            RecurringMeetingType.EveryMonth -> OccurrenceFrequencyType.Monthly
            else -> OccurrenceFrequencyType.Invalid
        }
}