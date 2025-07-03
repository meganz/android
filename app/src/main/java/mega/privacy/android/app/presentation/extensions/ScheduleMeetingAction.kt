package mega.privacy.android.app.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.icon.pack.IconPack

internal val ScheduleMeetingAction.title: Int
    get() = when (this) {
        ScheduleMeetingAction.Recurrence -> R.string.meetings_schedule_meeting_recurrence_label
        ScheduleMeetingAction.EndRecurrence -> R.string.meetings_schedule_meeting_end_recurrence_label
        ScheduleMeetingAction.MeetingLink -> R.string.meeting_link
        ScheduleMeetingAction.AddParticipants -> R.string.add_participants_menu_item
        ScheduleMeetingAction.SendCalendarInvite -> R.string.meetings_schedule_meeting_send_calendar_invite_label
        ScheduleMeetingAction.AllowNonHostAddParticipants -> R.string.chat_group_chat_info_allow_non_host_participants_option
        ScheduleMeetingAction.AddDescription -> R.string.meetings_schedule_meeting_add_description_label
        ScheduleMeetingAction.WaitingRoom -> R.string.meetings_schedule_meeting_waiting_room_label
    }

internal val ScheduleMeetingAction.description: Int?
    get() = when (this) {
        ScheduleMeetingAction.Recurrence -> null
        ScheduleMeetingAction.EndRecurrence -> null
        ScheduleMeetingAction.MeetingLink -> R.string.scheduled_meetings_share_meeting_link_panel_title
        ScheduleMeetingAction.AddParticipants -> null
        ScheduleMeetingAction.SendCalendarInvite -> R.string.scheduled_meetings_send_calendar_invite_panel_title
        ScheduleMeetingAction.AllowNonHostAddParticipants -> null
        ScheduleMeetingAction.AddDescription -> null
        ScheduleMeetingAction.WaitingRoom -> R.string.meetings_schedule_meeting_waiting_room_description
    }

internal val ScheduleMeetingAction.icon: ImageVector?
    get() = when (this) {
        ScheduleMeetingAction.Recurrence -> IconPack.Medium.Regular.Outline.RotateCw
        ScheduleMeetingAction.EndRecurrence -> null
        ScheduleMeetingAction.MeetingLink -> IconPack.Medium.Regular.Outline.Link01
        ScheduleMeetingAction.AddParticipants -> IconPack.Medium.Regular.Outline.UserPlus
        ScheduleMeetingAction.SendCalendarInvite -> IconPack.Medium.Regular.Outline.CalendarArrowRight
        ScheduleMeetingAction.AllowNonHostAddParticipants -> IconPack.Medium.Regular.Outline.Users
        ScheduleMeetingAction.AddDescription -> IconPack.Medium.Regular.Outline.Menu04
        ScheduleMeetingAction.WaitingRoom -> IconPack.Medium.Regular.Outline.ClockUser
    }