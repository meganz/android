package mega.privacy.android.app.constants

object EventConstants {
    const val EVENT_CONTACT_NAME_CHANGE = "contact_name_change"
    const val EVENT_MEETING_AVATAR_CHANGE = "meeting_avatar_change"
    const val EVENT_MEETING_GET_AVATAR = "meeting_get_avatar"
    const val EVENT_CHAT_OPEN_INVITE = "chat_open_invite"

    /** Event Keys related to calls*/
    const val EVENT_NOT_OUTGOING_CALL = "not_outgoing_call"
    const val EVENT_OUTGOING_CALL = "outgoing_call"
    const val EVENT_ENTER_IN_MEETING = "enter_in_meeting"
    const val EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT = "call_answered_in_another_client"
    const val EVENT_AUDIO_OUTPUT_CHANGE = "audio_output_change"
    const val EVENT_MEETING_CREATED = "meeting_created"
    const val EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE = "enable_or_disable_local_video_change"
    const val EVENT_REMOVE_CALL_NOTIFICATION = "remove_call_notification"
    const val EVENT_UPDATE_WAITING_FOR_OTHERS = "update_waiting_for_others"

    /** Event Keys related to sessions*/
    const val EVENT_PRIVILEGES_CHANGE = "privileges_in_char_change"
    const val EVENT_USER_VISIBILITY_CHANGE = "user_visibility_change"
    const val EVENT_CHAT_CONNECTION_STATUS = "chat_connection_status_change"
    const val EVENT_SHOW_SCANNING_TRANSFERS_DIALOG = "EVENT_SHOW_SCANNING_TRANSFERS_DIALOG"
    const val EVENT_SCANNING_TRANSFERS_CANCELLED = "EVENT_SCANNING_TRANSFERS_CANCELLED"
}