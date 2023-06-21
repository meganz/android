package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

internal val DropdownOccurrenceType.StringId: Int
    get() = when (this) {
        DropdownOccurrenceType.Day -> R.plurals.retention_time_picker_days
        DropdownOccurrenceType.Week -> R.plurals.retention_time_picker_weeks
        DropdownOccurrenceType.Month -> R.plurals.retention_time_picker_months
    }

internal val DropdownOccurrenceType.MaximumValue: Int
    get() = when (this) {
        DropdownOccurrenceType.Day -> 99
        DropdownOccurrenceType.Week -> 52
        DropdownOccurrenceType.Month -> 12
    }

internal val DropdownOccurrenceType.OccurrenceType: OccurrenceFrequencyType
    get() = when (this) {
        DropdownOccurrenceType.Day -> OccurrenceFrequencyType.Daily
        DropdownOccurrenceType.Week -> OccurrenceFrequencyType.Weekly
        DropdownOccurrenceType.Month -> OccurrenceFrequencyType.Monthly
    }
