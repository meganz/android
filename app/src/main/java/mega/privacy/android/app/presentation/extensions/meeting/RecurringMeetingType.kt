package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.RecurringMeetingType

internal val RecurringMeetingType.StringId: Int
    get() = when (this) {
        RecurringMeetingType.Never -> R.string.meetings_schedule_meeting_recurrence_never_label
        RecurringMeetingType.EveryDay -> R.string.meetings_schedule_meeting_recurrence_dialog_every_day_option
        RecurringMeetingType.EveryWeek -> R.string.meetings_schedule_meeting_recurrence_dialog_every_week_option
        RecurringMeetingType.EveryMonth -> R.string.meetings_schedule_meeting_recurrence_dialog_every_month_option
        RecurringMeetingType.Custom -> R.string.meetings_schedule_meeting_recurrence_dialog_custom_option
        RecurringMeetingType.Customised -> R.string.meetings_schedule_meeting_recurrence_dialog_custom_option
    }
