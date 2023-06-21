package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption

internal val RecurrenceDialogOption.StringId: Int
    get() = when (this) {
        RecurrenceDialogOption.Never -> R.string.meetings_schedule_meeting_recurrence_never_label
        RecurrenceDialogOption.EveryDay -> R.string.meetings_schedule_meeting_recurrence_dialog_every_day_option
        RecurrenceDialogOption.EveryWeek -> R.string.meetings_schedule_meeting_recurrence_dialog_every_week_option
        RecurrenceDialogOption.EveryMonth -> R.string.meetings_schedule_meeting_recurrence_dialog_every_month_option
        RecurrenceDialogOption.Custom -> R.string.meetings_schedule_meeting_recurrence_dialog_custom_option
        RecurrenceDialogOption.Customised -> R.string.meetings_schedule_meeting_recurrence_dialog_custom_option
    }
