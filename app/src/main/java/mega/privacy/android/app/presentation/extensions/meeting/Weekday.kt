package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.Weekday

internal val Weekday.StringId: Int
    get() = when (this) {
        Weekday.Monday -> R.string.meetings_custom_recurrence_monday_option
        Weekday.Tuesday -> R.string.meetings_custom_recurrence_tuesday_option
        Weekday.Wednesday -> R.string.meetings_custom_recurrence_wednesday_option
        Weekday.Thursday -> R.string.meetings_custom_recurrence_thursday_option
        Weekday.Friday -> R.string.meetings_custom_recurrence_friday_option
        Weekday.Saturday -> R.string.meetings_custom_recurrence_saturday_option
        Weekday.Sunday -> R.string.meetings_custom_recurrence_sunday_option
    }