package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction

internal val ScheduledMeetingInfoAction.title: Int
    get() = when (this) {
        ScheduledMeetingInfoAction.MeetingLink -> R.string.meeting_link
        ScheduledMeetingInfoAction.ShareMeetingLink -> R.string.meetings_scheduled_meeting_info_share_meeting_link_label
        ScheduledMeetingInfoAction.ChatNotifications -> R.string.meetings_info_notifications_option
        ScheduledMeetingInfoAction.WaitingRoom -> R.string.meetings_schedule_meeting_waiting_room_label
        ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> R.string.chat_group_chat_info_allow_non_host_participants_option
        ScheduledMeetingInfoAction.ShareFiles -> R.string.title_chat_shared_files_info
        ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts -> R.string.meetings_scheduled_meeting_info_share_meeting_link_label
        ScheduledMeetingInfoAction.ManageChatHistory -> R.string.meetings_info_manage_history_option
        ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> R.string.make_chat_private_option
        ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation -> R.string.private_chat
    }

internal val ScheduledMeetingInfoAction.description: Int?
    get() = when (this) {
        ScheduledMeetingInfoAction.MeetingLink -> null
        ScheduledMeetingInfoAction.ShareMeetingLink -> null
        ScheduledMeetingInfoAction.ChatNotifications -> null
        ScheduledMeetingInfoAction.WaitingRoom -> R.string.meetings_schedule_meeting_waiting_room_description
        ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> null
        ScheduledMeetingInfoAction.ShareFiles -> null
        ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts -> null
        ScheduledMeetingInfoAction.ManageChatHistory -> null
        ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> R.string.make_chat_private_option_text
        ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation -> R.string.make_chat_private_option_text
    }

internal val ScheduledMeetingInfoAction.icon: Int?
    get() = when (this) {
        ScheduledMeetingInfoAction.MeetingLink -> R.drawable.ic_link
        ScheduledMeetingInfoAction.ShareMeetingLink -> null
        ScheduledMeetingInfoAction.ChatNotifications -> R.drawable.ic_bell
        ScheduledMeetingInfoAction.WaitingRoom -> R.drawable.waiting_room_ic
        ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> R.drawable.ic_users
        ScheduledMeetingInfoAction.ShareFiles -> R.drawable.ic_share_files
        ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts -> CoreUiR.drawable.link_ic
        ScheduledMeetingInfoAction.ManageChatHistory -> R.drawable.ic_manage_history
        ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> null
        ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation -> null
    }