package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction

internal val ScheduleMeetingAction.title: Int
    get() = when (this) {
        ScheduleMeetingAction.Recurrence -> R.string.meetings_schedule_meeting_recurrence_label
        ScheduleMeetingAction.MeetingLink -> R.string.meeting_link
        ScheduleMeetingAction.AddParticipants -> R.string.add_participants_menu_item
        ScheduleMeetingAction.SendCalendarInvite -> R.string.meetings_schedule_meeting_send_calendar_invite_label
        ScheduleMeetingAction.AllowNonHostAddParticipants -> R.string.chat_group_chat_info_allow_non_host_participants_option
        ScheduleMeetingAction.AddDescription -> R.string.meetings_schedule_meeting_add_description_label
    }

internal val ScheduleMeetingAction.description: Int?
    get() = when (this) {
        ScheduleMeetingAction.Recurrence -> null
        ScheduleMeetingAction.MeetingLink -> R.string.scheduled_meetings_share_meeting_link_panel_title
        ScheduleMeetingAction.AddParticipants -> null
        ScheduleMeetingAction.SendCalendarInvite -> null
        ScheduleMeetingAction.AllowNonHostAddParticipants -> null
        ScheduleMeetingAction.AddDescription -> null
    }

internal val ScheduleMeetingAction.icon: Int
    get() = when (this) {
        ScheduleMeetingAction.Recurrence -> R.drawable.ic_recurrence
        ScheduleMeetingAction.MeetingLink -> R.drawable.ic_meeting_link_info
        ScheduleMeetingAction.AddParticipants -> R.drawable.add_participants
        ScheduleMeetingAction.SendCalendarInvite -> R.drawable.ic_send_calendar
        ScheduleMeetingAction.AllowNonHostAddParticipants -> R.drawable.ic_users
        ScheduleMeetingAction.AddDescription -> R.drawable.ic_sched_meeting_description
    }