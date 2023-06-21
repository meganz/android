package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption


internal val OccurrenceFrequencyType.DropdownType: DropdownOccurrenceType
    get() = when (this) {
        OccurrenceFrequencyType.Invalid, OccurrenceFrequencyType.Daily -> DropdownOccurrenceType.Day
        OccurrenceFrequencyType.Weekly -> DropdownOccurrenceType.Week
        OccurrenceFrequencyType.Monthly -> DropdownOccurrenceType.Month
    }

internal val OccurrenceFrequencyType.DialogOption: RecurrenceDialogOption
    get() = when (this) {
        OccurrenceFrequencyType.Invalid -> RecurrenceDialogOption.Never
        OccurrenceFrequencyType.Daily -> RecurrenceDialogOption.EveryDay
        OccurrenceFrequencyType.Weekly -> RecurrenceDialogOption.EveryWeek
        OccurrenceFrequencyType.Monthly -> RecurrenceDialogOption.EveryMonth
    }