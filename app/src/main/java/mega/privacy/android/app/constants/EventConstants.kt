package mega.privacy.android.app.constants

object EventConstants {
    const val EVENT_TEXT_FILE_UPLOADED = "EVENT_TEXT_FILE_UPLOADED"
    const val EVENT_REFRESH = "EVENT_REFRESH"

    const val EVENT_CHAT_TITLE_CHANGE = "chat_title_change"
    const val EVENT_CONTACT_NAME_CHANGE = "contact_name_change"
    const val EVENT_MEETING_AVATAR_CHANGE = "meeting_avatar_change"
    const val EVENT_MEETING_GET_AVATAR = "meeting_get_avatar"
    const val EVENT_CHAT_OPEN_INVITE = "chat_open_invite"

    /** Event Keys related to calls*/
    const val EVENT_ERROR_STARTING_CALL = "error_starting_call"
    const val EVENT_UPDATE_CALL = "update_call"
    const val EVENT_CALL_STATUS_CHANGE = "call_status_change"
    const val EVENT_LOCAL_AVFLAGS_CHANGE = "local_avflags_change"
    const val EVENT_RINGING_STATUS_CHANGE = "ringing_status_change"
    const val EVENT_CALL_COMPOSITION_CHANGE = "call_composition_change"
    const val EVENT_CALL_ON_HOLD_CHANGE = "call_on_hold_change"
    const val EVENT_CALL_SPEAK_CHANGE = "call_speak_change"
    const val EVENT_LOCAL_AUDIO_LEVEL_CHANGE = "local_audio_level_change"
    const val EVENT_LOCAL_NETWORK_QUALITY_CHANGE = "local_network_quality_change"
    const val EVENT_NOT_OUTGOING_CALL = "not_outgoing_call"
    const val EVENT_OUTGOING_CALL = "outgoing_call"
    const val EVENT_ENTER_IN_MEETING = "enter_in_meeting"
    const val EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT = "call_answered_in_another_client"
    const val EVENT_AUDIO_OUTPUT_CHANGE = "audio_output_change"
    const val EVENT_MEETING_CREATED = "meeting_created"
    const val EVENT_LINK_RECOVERED = "meeting_link_recovered"
    const val EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE = "enable_or_disable_local_video_change"
    const val EVENT_REMOVE_CALL_NOTIFICATION = "remove_call_notification"
    const val EVENT_CALL_OUTGOING_RINGING_CHANGE = "call_outgoing_ringing_change"
    const val EVENT_UPDATE_WAITING_FOR_OTHERS = "update_waiting_for_others"

    /** Event Keys related to sessions*/
    const val EVENT_SESSION_STATUS_CHANGE = "session_status_change"
    const val EVENT_REMOTE_AVFLAGS_CHANGE = "remote_avflags_change"
    const val EVENT_SESSION_SPEAK_REQUESTED = "session_speak_requested_change"
    const val EVENT_SESSION_ON_HIRES_CHANGE = "session_on_hires_change"
    const val EVENT_SESSION_ON_LOWRES_CHANGE = "session_on_lowres_change"
    const val EVENT_REMOTE_AUDIO_LEVEL_CHANGE = "remote_audio_level_change"
    const val EVENT_SESSION_ON_HOLD_CHANGE = "session_on_hold_change"

    const val EVENT_PRIVILEGES_CHANGE = "privileges_in_char_change"
    const val EVENT_USER_VISIBILITY_CHANGE = "user_visibility_change"
    const val EVENT_CHAT_CONNECTION_STATUS = "chat_connection_status_change"

    const val EVENT_UPDATE_SCROLL = "EVENT_UPDATE_SCROLL"
    const val EVENT_PERFORM_SCROLL = "EVENT_PERFORM_SCROLL"

    const val EVENT_UPDATE_VIEW_MODE = "EVENT_UPDATE_VIEW_MODE"
    const val EVENT_SHOW_MEDIA_DISCOVERY = "EVENT_SHOW_MEDIA_DISCOVERY"

    const val EVENT_SHOW_SCANNING_TRANSFERS_DIALOG = "EVENT_SHOW_SCANNING_TRANSFERS_DIALOG"
    const val EVENT_FINISH_SERVICE_IF_NO_TRANSFERS = "EVENT_FINISH_SERVICE_IF_NO_TRANSFERS"
    const val EVENT_SCANNING_TRANSFERS_CANCELLED = "EVENT_SCANNING_TRANSFERS_CANCELLED"
}