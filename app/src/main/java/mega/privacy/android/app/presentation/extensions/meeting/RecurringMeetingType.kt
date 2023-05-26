package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingType
import mega.privacy.android.app.R

internal val RecurringMeetingType.StringId: Int
    get() = when (this) {
        RecurringMeetingType.Never -> R.string.meetings_schedule_meeting_recurrence_never_label
        RecurringMeetingType.Daily -> R.string.meetings_schedule_meeting_recurrence_dialog_every_day_option
        RecurringMeetingType.Weekly -> R.string.meetings_schedule_meeting_recurrence_dialog_every_week_option
        RecurringMeetingType.Monthly -> R.string.meetings_schedule_meeting_recurrence_dialog_every_month_option
        RecurringMeetingType.Custom -> R.string.meetings_schedule_meeting_recurrence_dialog_custom_option
    }
