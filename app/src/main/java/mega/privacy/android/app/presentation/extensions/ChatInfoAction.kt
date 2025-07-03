package mega.privacy.android.app.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ChatInfoAction
import mega.privacy.android.icon.pack.IconPack

internal val ChatInfoAction.title: Int
    get() = when (this) {
        ChatInfoAction.MeetingLink -> R.string.meeting_link
        ChatInfoAction.ShareMeetingLink -> R.string.meetings_scheduled_meeting_info_share_meeting_link_label
        ChatInfoAction.ChatNotifications -> R.string.meetings_info_notifications_option
        ChatInfoAction.WaitingRoom -> R.string.meetings_schedule_meeting_waiting_room_label
        ChatInfoAction.AllowNonHostAddParticipants -> R.string.chat_group_chat_info_allow_non_host_participants_option
        ChatInfoAction.ShareFiles -> R.string.title_chat_shared_files_info
        ChatInfoAction.ShareMeetingLinkNonHosts -> R.string.meetings_scheduled_meeting_info_share_meeting_link_label
        ChatInfoAction.ManageMeetingHistory -> R.string.meetings_info_manage_history_option
        ChatInfoAction.EnableEncryptedKeyRotation -> R.string.make_chat_private_option
        ChatInfoAction.EnabledEncryptedKeyRotation -> R.string.private_chat
        ChatInfoAction.Archive -> R.string.general_archive
        ChatInfoAction.Unarchive -> R.string.general_unarchive
        ChatInfoAction.Files -> R.string.general_files
        ChatInfoAction.ManageChatHistory -> R.string.title_properties_manage_chat
    }

internal val ChatInfoAction.description: Int?
    get() = when (this) {
        ChatInfoAction.MeetingLink,
        ChatInfoAction.Files,
        ChatInfoAction.AllowNonHostAddParticipants,
        ChatInfoAction.ShareFiles,
        ChatInfoAction.ShareMeetingLinkNonHosts,
        ChatInfoAction.ManageChatHistory,
        ChatInfoAction.ManageMeetingHistory,
        ChatInfoAction.ShareMeetingLink,
        ChatInfoAction.ChatNotifications,
        ChatInfoAction.Archive,
        ChatInfoAction.Unarchive,
            -> null

        ChatInfoAction.WaitingRoom -> R.string.meetings_schedule_meeting_waiting_room_description
        ChatInfoAction.EnableEncryptedKeyRotation -> R.string.make_chat_private_option_text
        ChatInfoAction.EnabledEncryptedKeyRotation -> R.string.make_chat_private_option_text
    }

internal val ChatInfoAction.icon: ImageVector?
    get() = when (this) {
        ChatInfoAction.MeetingLink -> IconPack.Medium.Regular.Outline.Link01
        ChatInfoAction.ShareMeetingLink -> null
        ChatInfoAction.ChatNotifications -> IconPack.Medium.Regular.Outline.Bell
        ChatInfoAction.WaitingRoom -> IconPack.Medium.Regular.Outline.ClockUser
        ChatInfoAction.AllowNonHostAddParticipants -> IconPack.Medium.Regular.Outline.Users
        ChatInfoAction.ShareFiles, ChatInfoAction.Files -> IconPack.Medium.Regular.Outline.File02
        ChatInfoAction.ShareMeetingLinkNonHosts -> IconPack.Medium.Regular.Outline.Link01
        ChatInfoAction.ManageChatHistory, ChatInfoAction.ManageMeetingHistory -> IconPack.Medium.Regular.Outline.Eraser
        ChatInfoAction.EnableEncryptedKeyRotation -> null
        ChatInfoAction.EnabledEncryptedKeyRotation -> null
        ChatInfoAction.Archive -> IconPack.Medium.Regular.Outline.Archive
        ChatInfoAction.Unarchive -> IconPack.Medium.Regular.Outline.ArchiveArrowUp
    }