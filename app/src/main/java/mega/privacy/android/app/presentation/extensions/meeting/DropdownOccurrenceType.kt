package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType

internal val DropdownOccurrenceType.StringId: Int
    get() = when (this) {
        DropdownOccurrenceType.Day -> R.plurals.retention_time_picker_days
        DropdownOccurrenceType.Week -> R.plurals.retention_time_picker_weeks
        DropdownOccurrenceType.Month -> R.plurals.retention_time_picker_months
    }
