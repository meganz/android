package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import javax.inject.Inject

/**
 * Mapper to convert [RecurringMeetingType] to [OccurrenceFrequencyType]
 */
class OccurrenceFrequencyTypeMapper @Inject constructor() {
    operator fun invoke(recurringType: RecurringMeetingType): OccurrenceFrequencyType =
        when (recurringType) {
            RecurringMeetingType.Never -> OccurrenceFrequencyType.Invalid
            RecurringMeetingType.Daily -> OccurrenceFrequencyType.Daily
            RecurringMeetingType.Weekly -> OccurrenceFrequencyType.Weekly
            RecurringMeetingType.Monthly -> OccurrenceFrequencyType.Monthly
            else -> OccurrenceFrequencyType.Invalid
        }
}