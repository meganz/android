package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction


internal val ScheduledMeetingInfoAction.title: Int
    get() = when (this) {
        ScheduledMeetingInfoAction.MeetingLink -> R.string.meeting_link
        ScheduledMeetingInfoAction.ShareMeetingLink -> R.string.meeting_link
        ScheduledMeetingInfoAction.ChatNotifications -> R.string.title_properties_contact_notifications_for_chat
        ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> R.string.chat_group_chat_info_allow_non_host_participants_option
        ScheduledMeetingInfoAction.ShareFiles -> R.string.title_chat_shared_files_info
        ScheduledMeetingInfoAction.ManageChatHistory -> R.string.title_properties_manage_chat
    }

internal val ScheduledMeetingInfoAction.icon: Int?
    get() = when (this) {
        ScheduledMeetingInfoAction.MeetingLink -> R.drawable.ic_link
        ScheduledMeetingInfoAction.ShareMeetingLink -> null
        ScheduledMeetingInfoAction.ChatNotifications -> R.drawable.ic_bell
        ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> R.drawable.ic_users
        ScheduledMeetingInfoAction.ShareFiles -> R.drawable.ic_share_files
        ScheduledMeetingInfoAction.ManageChatHistory -> R.drawable.ic_manage_history
    }