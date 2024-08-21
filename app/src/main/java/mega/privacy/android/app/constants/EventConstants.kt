package mega.privacy.android.app.constants

object EventConstants {
    const val EVENT_CONTACT_NAME_CHANGE = "contact_name_change"
    const val EVENT_MEETING_AVATAR_CHANGE = "meeting_avatar_change"
    const val EVENT_MEETING_GET_AVATAR = "meeting_get_avatar"
    const val EVENT_CHAT_OPEN_INVITE = "chat_open_invite"

    /** Event Keys related to calls*/
    const val EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE = "enable_or_disable_local_video_change"
    const val EVENT_UPDATE_WAITING_FOR_OTHERS = "update_waiting_for_others"

    /** Event Keys related to sessions*/
    const val EVENT_PRIVILEGES_CHANGE = "privileges_in_char_change"
    const val EVENT_USER_VISIBILITY_CHANGE = "user_visibility_change"
    const val EVENT_CHAT_CONNECTION_STATUS = "chat_connection_status_change"
}