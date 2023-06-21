package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import javax.inject.Inject

/**
 * Mapper to convert [RecurrenceDialogOption] to [OccurrenceFrequencyType]
 */
class RecurrenceDialogOptionMapper @Inject constructor() {
    operator fun invoke(recurringType: RecurrenceDialogOption): OccurrenceFrequencyType =
        when (recurringType) {
            RecurrenceDialogOption.EveryDay -> OccurrenceFrequencyType.Daily
            RecurrenceDialogOption.EveryWeek -> OccurrenceFrequencyType.Weekly
            RecurrenceDialogOption.EveryMonth -> OccurrenceFrequencyType.Monthly
            else -> OccurrenceFrequencyType.Invalid
        }
}