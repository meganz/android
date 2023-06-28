package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.Weekday

internal val Weekday.InitialLetterStringId: Int
    get() = when (this) {
        Weekday.Monday -> R.string.meetings_custom_recurrence_monday_option
        Weekday.Tuesday -> R.string.meetings_custom_recurrence_tuesday_option
        Weekday.Wednesday -> R.string.meetings_custom_recurrence_wednesday_option
        Weekday.Thursday -> R.string.meetings_custom_recurrence_thursday_option
        Weekday.Friday -> R.string.meetings_custom_recurrence_friday_option
        Weekday.Saturday -> R.string.meetings_custom_recurrence_saturday_option
        Weekday.Sunday -> R.string.meetings_custom_recurrence_sunday_option
    }

internal val Weekday.StringId: Int
    get() = when (this) {
        Weekday.Monday -> R.string.meetings_custom_recurrence_monday_monthly_section
        Weekday.Tuesday -> R.string.meetings_custom_recurrence_tuesday_monthly_section
        Weekday.Wednesday -> R.string.meetings_custom_recurrence_wednesday_monthly_section
        Weekday.Thursday -> R.string.meetings_custom_recurrence_thursday_monthly_section
        Weekday.Friday -> R.string.meetings_custom_recurrence_friday_monthly_section
        Weekday.Saturday -> R.string.meetings_custom_recurrence_saturday_monthly_section
        Weekday.Sunday -> R.string.meetings_custom_recurrence_sunday_monthly_section
    }