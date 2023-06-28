package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.WeekOfMonth

internal val WeekOfMonth.StringId: Int
    get() = when (this) {
        WeekOfMonth.First -> R.string.meetings_custom_recurrence_first_week_monthly_section
        WeekOfMonth.Second -> R.string.meetings_custom_recurrence_second_week_monthly_section
        WeekOfMonth.Third -> R.string.meetings_custom_recurrence_third_week_monthly_section
        WeekOfMonth.Fourth -> R.string.meetings_custom_recurrence_fourth_week_monthly_section
        WeekOfMonth.Fifth -> R.string.meetings_custom_recurrence_fifth_week_monthly_section
    }