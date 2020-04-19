package mega.privacy.android.app.constants;

public class SettingsConstants {
    public static final String ACTION_REFRESH_CAMERA_UPLOADS_SETTING = "ACTION_REFRESH_CAMERA_UPLOADS_SETTING";
    public static final String ACTION_REFRESH_CLEAR_OFFLINE_SETTING = "ACTION_REFRESH_CLEAR_OFFLINE_SETTING";
    public static final int COMPRESSION_QUEUE_SIZE_MIN = 100;
    public static final int COMPRESSION_QUEUE_SIZE_MAX = 1000;

    public static final int REQUEST_CODE_TREE_LOCAL_CAMERA = 1050;
    public static final int REQUEST_CAMERA_FOLDER = 2000;
    public static final int REQUEST_MEGA_CAMERA_FOLDER = 3000;
    public static final int REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER = 4000;
    public static final int REQUEST_MEGA_SECONDARY_MEDIA_FOLDER = 5000;
    public static final String KEY_SET_QUEUE_DIALOG = "KEY_SET_QUEUE_DIALOG";
    public static final String KEY_SET_QUEUE_SIZE = "KEY_SET_QUEUE_SIZE";

    public static final int DEFAULT_CONVENTION_QUEUE_SIZE = 200;

    public static final String CATEGORY_PIN_LOCK = "settings_pin_lock";
    public static final String CATEGORY_CHAT_ENABLED = "settings_chat";
    public static final String CATEGORY_CHAT_NOTIFICATIONS = "settings_notifications_chat";
    public static final String CATEGORY_STORAGE = "settings_storage";
    public static final String CATEGORY_CAMERA_UPLOAD = "settings_camera_upload";
    public static final String CATEGORY_ADVANCED_FEATURES = "advanced_features";
    public static final String CATEGORY_QR_CODE = "settings_qrcode";
    public static final String CATEGORY_SECURITY = "settings_security";
    public static final String CATEGORY_2FA = "settings_2fa";
    public static final String CATEGORY_FILE_MANAGEMENT = "settings_file_management";

    public static final String KEY_QR_CODE_AUTO_ACCEPT = "settings_qrcode_autoaccept";
    public static final String KEY_2FA = "settings_2fa_activated";

    public static final String KEY_PIN_LOCK_ENABLE = "settings_pin_lock_enable";
    public static final String KEY_PIN_LOCK_CODE = "settings_pin_lock_code";

    public static final String KEY_CHAT_ENABLE = "settings_chat_enable";

    public static final String KEY_RICH_LINKS_ENABLE = "settings_rich_links_enable";

    public static final String CATEGORY_AUTOAWAY_CHAT = "settings_autoaway_chat";
    public static final String KEY_CHAT_AUTOAWAY = "settings_autoaway_chat_preference";
    public static final String KEY_AUTOAWAY_ENABLE = "settings_autoaway_chat_switch";

    public static final String CATEGORY_PERSISTENCE_CHAT = "settings_persistence_chat";
    public static final String KEY_CHAT_PERSISTENCE = "settings_persistence_chat_checkpreference";

    public static final String KEY_CHAT_NESTED_NOTIFICATIONS = "settings_nested_notifications_chat";

    public static final String KEY_STORAGE_DOWNLOAD_CATEGORY = "download_setting";
    public static final String KEY_STORAGE_DOWNLOAD = "settings_nested_download_location";
    public static final String KEY_STORAGE_DOWNLOAD_LOCATION = "settings_storage_download_location";
    public static final String KEY_STORAGE_ASK_ME_ALWAYS = "settings_storage_ask_me_always";
    public static final String KEY_CAMERA_UPLOAD_ON = "settings_camera_upload_on";
    public static final String KEY_CAMERA_UPLOAD_HOW_TO = "settings_camera_upload_how_to_upload";
    public static final String KEY_CAMERA_UPLOAD_CHARGING = "settings_camera_upload_charging";
    public static final String KEY_CAMERA_UPLOAD_INCLUDE_GPS = "settings_camera_upload_include_gps";
    public static final String KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE = "video_compression_queue_size";
    public static final String KEY_KEEP_FILE_NAMES = "settings_keep_file_names";
    public static final String KEY_CAMERA_UPLOAD_WHAT_TO = "settings_camera_upload_what_to_upload";
    public static final String KEY_CAMERA_UPLOAD_VIDEO_QUALITY = "settings_video_upload_quality";
    public static final String KEY_CAMERA_UPLOAD_CAMERA_FOLDER = "settings_local_camera_upload_folder";
    public static final String KEY_CAMERA_UPLOAD_CAMERA_FOLDER_SDCARD = "settings_local_camera_upload_folder_sdcard";
    public static final String KEY_CAMERA_UPLOAD_MEGA_FOLDER = "settings_mega_camera_folder";

    public static final String KEY_SECONDARY_MEDIA_FOLDER_ON = "settings_secondary_media_folder_on";
    public static final String KEY_LOCAL_SECONDARY_MEDIA_FOLDER = "settings_local_secondary_media_folder";
    public static final String KEY_MEGA_SECONDARY_MEDIA_FOLDER = "settings_mega_secondary_media_folder";

    public static final String KEY_CACHE = "settings_advanced_features_cache";
    public static final String KEY_CANCEL_ACCOUNT = "settings_advanced_features_cancel_account";
    public static final String KEY_OFFLINE = "settings_file_management_offline";
    public static final String KEY_RUBBISH = "settings_file_management_rubbish";
    public static final String KEY_FILE_VERSIONS = "settings_file_management_file_version";
    public static final String KEY_CLEAR_VERSIONS = "settings_file_management_clear_version";
    public static final String KEY_ENABLE_VERSIONS = "settings_file_versioning_switch";
    public static final String KEY_ENABLE_RB_SCHEDULER = "settings_rb_scheduler_switch";
    public static final String KEY_DAYS_RB_SCHEDULER = "settings_days_rb_scheduler";

    public static final String KEY_ENABLE_LAST_GREEN_CHAT = "settings_last_green_chat_switch";

    public static final String KEY_ABOUT_PRIVACY_POLICY = "settings_about_privacy_policy";
    public static final String KEY_ABOUT_TOS = "settings_about_terms_of_service";
    public static final String KEY_ABOUT_GDPR = "settings_about_gdpr";
    public static final String KEY_ABOUT_SDK_VERSION = "settings_about_sdk_version";
    public static final String KEY_ABOUT_KARERE_VERSION = "settings_about_karere_version";
    public static final String KEY_ABOUT_APP_VERSION = "settings_about_app_version";
    public static final String KEY_ABOUT_CODE_LINK = "settings_about_code_link";

    public static final String KEY_HELP_SEND_FEEDBACK= "settings_help_send_feedfack";
    public static final String KEY_AUTO_PLAY_SWITCH= "auto_play_switch";

    public static final String KEY_RECOVERY_KEY= "settings_recovery_key";
    public static final String KEY_CHANGE_PASSWORD= "settings_change_password";

    public static final String CAMERA_UPLOADS_STATUS = "CAMERA_UPLOADS_STATUS";

    public static final int CAMERA_UPLOAD_WIFI_OR_DATA_PLAN = 1001;
    public static final int CAMERA_UPLOAD_WIFI = 1002;

    public static final int CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS = 1001;
    public static final int CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS = 1002;
    public static final int CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS = 1003;
    public static final int VIDEO_QUALITY_ORIGINAL = 0;
    public static final int VIDEO_QUALITY_MEDIUM = 1;

    public static final int STORAGE_DOWNLOAD_LOCATION_INTERNAL_SD_CARD = 1001;
    public static final int STORAGE_DOWNLOAD_LOCATION_EXTERNAL_SD_CARD = 1002;

    /**
     * Chat settings
     */
    public static final String KEY_CHAT_NOTIFICATIONS = "settings_chat_notifications";
    public static final String KEY_CHAT_SOUND = "settings_chat_sound";
    public static final String KEY_CHAT_VIBRATE = "settings_chat_vibrate";
}
