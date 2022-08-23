package mega.privacy.android.app.constants

/**
 * The constants regarding settings
 */
object SettingsConstants {
    /* General settings */

    const val KEY_FEATURES = "settings_features"
    const val KEY_FEATURES_CAMERA_UPLOAD = "settings_features_camera_upload"
    const val KEY_FEATURES_CHAT = "settings_features_chat"
    const val CATEGORY_STORAGE = "settings_storage"
    const val KEY_STORAGE_DOWNLOAD = "settings_nested_download_location"
    const val KEY_STORAGE_FILE_MANAGEMENT = "settings_storage_file_management"
    const val CATEGORY_SECURITY = "settings_security"
    const val KEY_RECOVERY_KEY = "settings_recovery_key"
    const val KEY_PASSCODE_LOCK = "settings_passcode_lock"
    const val KEY_CHANGE_PASSWORD = "settings_change_password"
    const val KEY_2FA = "settings_2fa_activated"
    const val KEY_QR_CODE_AUTO_ACCEPT = "settings_qrcode_autoaccept"
    const val KEY_SECURITY_ADVANCED = "settings_security_advanced"
    const val KEY_HELP_CENTRE = "settings_help_centre"
    const val KEY_HELP_SEND_FEEDBACK = "settings_help_send_feedback"
    const val CATEGORY_ABOUT = "settings_about"
    const val KEY_ABOUT_PRIVACY_POLICY = "settings_about_privacy_policy"
    const val KEY_ABOUT_COOKIE_POLICY = "settings_about_cookie_policy"
    const val KEY_COOKIE_SETTINGS = "settings_cookie"
    const val KEY_ABOUT_TOS = "settings_about_terms_of_service"
    const val KEY_ABOUT_CODE_LINK = "settings_about_code_link"
    const val KEY_ABOUT_SDK_VERSION = "settings_about_sdk_version"
    const val KEY_ABOUT_KARERE_VERSION = "settings_about_karere_version"
    const val KEY_ABOUT_APP_VERSION = "settings_about_app_version"
    const val KEY_CANCEL_ACCOUNT = "settings_about_cancel_account"
    const val KEY_AUDIO_BACKGROUND_PLAY_ENABLED = "settings_audio_background_play_enabled"
    const val KEY_AUDIO_SHUFFLE_ENABLED = "settings_audio_shuffle_enabled"
    const val KEY_AUDIO_REPEAT_MODE = "settings_audio_repeat_mode"
    const val KEY_VIDEO_REPEAT_MODE = "settings_video_repeat_mode"
    const val KEY_FEATURES_CALLS = "settings_features_calls"

    /* CU settings */
    const val KEY_CAMERA_UPLOAD_ON_OFF = "settings_camera_upload_on_off"
    const val KEY_CAMERA_UPLOAD_HOW_TO = "settings_camera_upload_how_to_upload"
    const val KEY_CAMERA_UPLOAD_WHAT_TO = "settings_camera_upload_what_to_upload"
    const val KEY_CAMERA_UPLOAD_INCLUDE_GPS = "settings_camera_upload_include_gps"
    const val KEY_CAMERA_UPLOAD_VIDEO_QUALITY = "settings_video_upload_quality"
    const val KEY_CAMERA_UPLOAD_CHARGING = "settings_camera_upload_charging"
    const val KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE = "video_compression_queue_size"
    const val KEY_KEEP_FILE_NAMES = "settings_keep_file_names"
    const val KEY_CAMERA_UPLOAD_CAMERA_FOLDER = "settings_local_camera_upload_folder"
    const val KEY_CAMERA_UPLOAD_MEGA_FOLDER = "settings_mega_camera_folder"
    const val KEY_SECONDARY_MEDIA_FOLDER_ON = "settings_secondary_media_folder_on"
    const val KEY_LOCAL_SECONDARY_MEDIA_FOLDER = "settings_local_secondary_media_folder"
    const val KEY_MEGA_SECONDARY_MEDIA_FOLDER = "settings_mega_secondary_media_folder"
    const val DEFAULT_CONVENTION_QUEUE_SIZE = 200
    const val COMPRESSION_QUEUE_SIZE_MIN = 100
    const val COMPRESSION_QUEUE_SIZE_MAX = 1000
    const val REQUEST_CAMERA_FOLDER = 2000
    const val REQUEST_MEGA_CAMERA_FOLDER = 3000
    const val REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER = 4000
    const val REQUEST_MEGA_SECONDARY_MEDIA_FOLDER = 5000
    const val KEY_SET_QUEUE_DIALOG = "KEY_SET_QUEUE_DIALOG"
    const val KEY_SET_QUEUE_SIZE = "KEY_SET_QUEUE_SIZE"
    const val SELECTED_MEGA_FOLDER = "SELECT_MEGA_FOLDER"
    const val CAMERA_UPLOAD_WIFI_OR_DATA_PLAN = 1001
    const val CAMERA_UPLOAD_WIFI = 1002
    const val CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS = 1001
    const val CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS = 1002
    const val CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS = 1003
    const val INVALID_PATH = ""

    /* Chat settings */
    const val KEY_CHAT_NOTIFICATIONS_CHAT = "settings_chat_notification_chat"
    const val KEY_CHAT_STATUS = "settings_chat_list_status"
    const val KEY_CHAT_AUTOAWAY_SWITCH = "settings_chat_autoaway_switch"
    const val KEY_CHAT_AUTOAWAY_PREFERENCE = "settings_chat_autoaway_preference"
    const val KEY_CHAT_PERSISTENCE = "settings_chat_persistence"
    const val KEY_CHAT_LAST_GREEN = "settings_chat_last_green"
    const val KEY_CHAT_SEND_ORIGINALS = "settings_chat_send_originals"
    const val KEY_CHAT_RICH_LINK = "settings_chat_rich_links_enable"

    /* Chat notifications settings */
    const val KEY_CHAT_NOTIFICATIONS = "settings_chat_notifications"
    const val KEY_CHAT_SOUND = "settings_chat_sound"
    const val KEY_CHAT_VIBRATE = "settings_chat_vibrate"
    const val KEY_CHAT_DND = "settings_chat_dnd"

    /* Download settings */
    const val KEY_STORAGE_DOWNLOAD_LOCATION = "settings_storage_download_location"
    const val KEY_STORAGE_ASK_ME_ALWAYS = "settings_storage_ask_me_always"

    /* File management settings */
    const val KEY_OFFLINE = "settings_file_management_offline"
    const val KEY_CACHE = "settings_advanced_features_cache"
    const val KEY_RUBBISH = "settings_file_management_rubbish"
    const val KEY_ENABLE_RB_SCHEDULER = "settings_rb_scheduler_switch"
    const val KEY_DAYS_RB_SCHEDULER = "settings_days_rb_scheduler"
    const val KEY_ENABLE_VERSIONS = "settings_file_versioning_switch"
    const val KEY_FILE_VERSIONS = "settings_file_management_file_version"
    const val KEY_CLEAR_VERSIONS = "settings_file_management_clear_version"
    const val KEY_AUTO_PLAY_SWITCH = "auto_play_switch"
    const val KEY_MOBILE_DATA_HIGH_RESOLUTION = "setting_mobile_data_high_resolution"

    /* User interface settings */
    const val KEY_START_SCREEN = "settings_start_screen"
    const val KEY_HIDE_RECENT_ACTIVITY = "settings_hide_recent_activity"

    /* PassCode Lock settings */
    const val KEY_PASSCODE_ENABLE = "settings_passcode_enable"
    const val KEY_RESET_PASSCODE = "settings_change_passcode"
    const val KEY_FINGERPRINT_ENABLE = "settings_fingerprint_enable"
    const val KEY_REQUIRE_PASSCODE = "settings_require_passcode"

    /* Cookie settings  */
    const val KEY_COOKIE_ACCEPT = "settings_cookie_accept"
    const val KEY_COOKIE_ESSENTIAL = "settings_cookie_essential"
    const val KEY_COOKIE_ANALYTICS = "settings_cookie_performance_analytics"
    const val KEY_COOKIE_POLICIES = "setting_cookie_policies"

    const val REPORT_ISSUE = "settings_help_report_issue"
}