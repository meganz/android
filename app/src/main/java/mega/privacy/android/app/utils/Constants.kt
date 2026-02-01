package mega.privacy.android.app.utils

import mega.privacy.android.app.BuildConfig
import java.io.File
import java.util.regex.Pattern

object Constants {
    const val PIN_4: String = "4"
    const val PIN_6: String = "6"
    const val PIN_ALPHANUMERIC: String = "alphanumeric"

    const val DEFAULT_AVATAR_WIDTH_HEIGHT: Int = 250 //in pixels

    const val REQUEST_CODE_SELECT_FOLDER_TO_MOVE: Int = 1001
    const val REQUEST_CODE_SELECT_FOLDER_TO_COPY: Int = 1002
    const val REQUEST_CODE_REFRESH: Int = 1005
    const val REQUEST_CODE_SELECT_IMPORT_FOLDER: Int = 1007
    const val REQUEST_CODE_SELECT_CONTACT: Int = 1009
    const val TAKE_PHOTO_CODE: Int = 1010
    const val TAKE_PICTURE_PROFILE_CODE: Int = 1015
    const val REQUEST_CREATE_CHAT: Int = 1018
    const val REQUEST_ADD_PARTICIPANTS: Int = 1019
    const val REQUEST_CODE_SELECT_CHAT: Int = 1025
    const val REQUEST_CODE_FILE_INFO: Int = 1027
    const val REQUEST_CODE_REFRESH_API_SERVER: Int = 1028
    const val REQUEST_CODE_GET_FOLDER: Int = 1038
    const val REQUEST_CODE_GET_FOLDER_CONTENT: Int = 1039

    const val ACTION_REFRESH_AFTER_BLOCKED: String = "ACTION_REFRESH_AFTER_BLOCKED"
    const val ACTION_REFRESH: String = "ACTION_REFRESH"
    const val ACTION_REFRESH_API_SERVER: String = "ACTION_REFRESH_API_SERVER"
    const val ACTION_CONFIRM: String = "MEGA_ACTION_CONFIRM"
    const val EXTRA_CONFIRMATION: String = "MEGA_EXTRA_CONFIRMATION"

    const val ACTION_FORWARD_MESSAGES: String = "ACTION_FORWARD_MESSAGES"
    const val ACTION_OPEN_QR: String = "ACTION_OPEN_QR"
    const val ACTION_TAKE_PICTURE: String = "ACTION_TAKE_PICTURE"
    const val ACTION_TAKE_PROFILE_PICTURE: String = "ACTION_TAKE_PROFILE_PICTURE"
    const val ACTION_PREVIEW_GIPHY: String = "ACTION_PREVIEW_GIPHY"

    const val SHOW_REPEATED_UPLOAD: String = "SHOW_REPEATED_UPLOAD"

    const val EXTRA_SERIALIZE_STRING: String = "SERIALIZE_STRING"

    const val EXTRA_USER_NICKNAME: String = "EXTRA_USER_NICKNAME"

    const val FROM_HOME_PAGE: String = "FROM_HOME_PAGE"

    const val RESULT: String = "RESULT"
    const val ACCOUNT_BLOCKED_STRING: String = "ACCOUNT_BLOCKED_STRING"
    const val ACCOUNT_BLOCKED_TYPE: String = "ACCOUNT_BLOCKED_TYPE"
    const val ACTION_SHOW_WARNING_ACCOUNT_BLOCKED: String = "ACTION_SHOW_WARNING_ACCOUNT_BLOCKED"


    const val EXTRA_MOVE_TO_CHAT_SECTION: String = "EXTRA_MOVE_TO_CHAT_SECTION"

    //MultipleRequestListener options
    const val MULTIPLE_SEND_RUBBISH: Int = 1
    const val MULTIPLE_LEAVE_SHARE: Int = 8

    const val CANCEL_ACCOUNT_2FA: Int = 4000
    const val CHANGE_MAIL_2FA: Int = 4001
    const val DISABLE_2FA: Int = 4002
    const val CHANGE_PASSWORD_2FA: Int = 4003

    const val TOUR_FRAGMENT: Int = 6000
    const val LOGIN_FRAGMENT: Int = 6001
    const val CONFIRM_EMAIL_FRAGMENT: Int = 6002
    const val CREATE_ACCOUNT_FRAGMENT: Int = 604


    const val UPDATE_ACCOUNT_DETAILS: Int = 9003

    const val REQUEST_WRITE_STORAGE: Int = 1
    const val REQUEST_CAMERA: Int = 2
    const val REQUEST_READ_CONTACTS: Int = 3
    const val REQUEST_RECORD_AUDIO: Int = 4

    const val REQUEST_READ_WRITE_STORAGE: Int = 9

    const val IMPORT_ONLY_OPTION: Int = 0
    const val IMPORT_TO_SHARE_OPTION: Int = 2

    const val BUSINESS: Int = 100
    const val PRO_FLEXI: Int = 101

    const val DISABLED_RETENTION_TIME: Long = 0
    const val SECONDS_IN_MINUTE: Long = 60
    const val SECONDS_IN_HOUR: Int = 3600
    const val SECONDS_IN_DAY: Int = SECONDS_IN_HOUR * 24
    const val SECONDS_IN_WEEK: Int = SECONDS_IN_DAY * 7
    const val SECONDS_IN_MONTH_30: Int = SECONDS_IN_DAY * 30
    const val SECONDS_IN_YEAR: Int = SECONDS_IN_DAY * 365

    const val SECONDS_TO_WAIT_ALONE_ON_THE_CALL: Long = 2 * SECONDS_IN_MINUTE
    const val SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL: Long = 5 * SECONDS_IN_MINUTE

    const val DISPUTE_URL: String = "https://mega.io/dispute"
    const val TAKEDOWN_URL: String = "https://mega.io/takedown"
    const val TERMS_OF_SERVICE_URL: String = "https://mega.io/terms"
    const val HELP_CENTRE_HOME_URL: String = "https://help.mega.io"
    const val ACTION_OPEN_MEGA_LINK: String = "OPEN_MEGA_LINK"
    const val ACTION_OPEN_MEGA_FOLDER_LINK: String = "OPEN_MEGA_FOLDER_LINK"
    const val ACTION_IMPORT_LINK_FETCH_NODES: String = "IMPORT_LINK_FETCH_NODES"
    const val ACTION_FILE_EXPLORER_UPLOAD: String = "FILE_EXPLORER_UPLOAD"
    const val ACTION_EXPLORE_ZIP: String = "EXPLORE_ZIP"
    const val EXTRA_PATH_ZIP: String = "PATH_ZIP"
    const val EXTRA_HANDLE_ZIP: String = "HANDLE_ZIP"
    const val EXTRA_OPEN_FOLDER: String = "EXTRA_OPEN_FOLDER"
    const val ACTION_OVER_QUOTA_STORAGE: String = "OVERQUOTA_STORAGE"
    const val ACTION_TAKE_SELFIE: String = "TAKE_SELFIE"
    const val ACTION_LOCATE_DOWNLOADED_FILE: String = "LOCATE_DOWNLOADED_FILE"
    const val ACTION_EXPORT_MASTER_KEY: String = "EXPORT_MASTER_KEY"
    const val ACTION_OPEN_FOLDER: String = "OPEN_FOLDER"
    const val ACTION_CANCEL_ACCOUNT: String = "CANCEL_ACCOUNT"
    const val ACTION_RESET_PASS: String = "RESET_PASS"
    const val ACTION_OPEN_USAGE_METER_FROM_MENU: String = "OPEN_USAGE_METER_FROM_MENU"
    const val ACTION_RESET_PASS_FROM_LINK: String = "RESET_PASS_FROM_LINK"
    const val ACTION_PASS_CHANGED: String = "PASS_CHANGED"
    const val ACTION_RESET_PASS_FROM_PARK_ACCOUNT: String = "RESET_PASS_FROM_PARK_ACCOUNT"
    const val ACTION_CHANGE_MAIL: String = "CHANGE_MAIL"
    const val ACTION_IPC: String = "IPC"
    const val ACTION_SHOW_MY_ACCOUNT: String = "ACTION_SHOW_MY_ACCOUNT"
    const val ACTION_CHAT_NOTIFICATION_MESSAGE: String = "ACTION_CHAT_MESSAGE"
    const val ACTION_CHAT_SUMMARY: String = "ACTION_CHAT_SUMMARY"
    const val ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION: String =
        "ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION"
    const val ACTION_OPEN_HANDLE_NODE: String = "ACTION_OPEN_HANDLE_NODE"
    const val ACTION_OPEN_FILE_LINK_ROOT_NODES_NULL: String = "ACTION_OPEN_FILE_LINK_ROOTNODES_NULL"
    const val ACTION_OPEN_FOLDER_LINK_ROOT_NODES_NULL: String =
        "ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL"
    const val ACTION_SHOW_SETTINGS: String = "ACTION_SHOW_SETTINGS"
    const val ACTION_PRE_OVER_QUOTA_STORAGE: String = "PRE_OVERQUOTA_STORAGE"
    const val ACTION_LOG_IN: String = "ACTION_LOG_IN"
    const val ACTION_OPEN_DEVICE_CENTER: String = "ACTION_OPEN_DEVICE_CENTER"
    const val ACTION_OPEN_SYNC_MEGA_FOLDER: String = "ACTION_OPEN_SYNC_MEGA_FOLDER"
    const val ACTION_SHOW_WARNING: String = "ACTION_SHOW_WARNING"
    const val OPENED_FROM_CHAT: String = "OPENED_FROM_CHAT"
    const val ACTION_OPEN_CHAT_LINK: String = "OPEN_CHAT_LINK"
    const val ACTION_JOIN_OPEN_CHAT_LINK: String = "JOIN_OPEN_CHAT_LINK"
    const val ACTION_CHAT_SHOW_MESSAGES: String = "CHAT_SHOW_MESSAGES"
    const val ACTION_SHOW_UPGRADE_ACCOUNT: String = "ACTION_SHOW_UPGRADE_ACCOUNT"
    const val ACTION_OPEN_CONTACTS_SECTION: String = "ACTION_OPEN_CONTACTS_SECTION"

    const val ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE: String = "ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE"

    const val ACTION_SHARE_MSG: String = "ACTION_SHARE_MSG"
    const val ACTION_SHARE_NODE: String = "ACTION_SHARE_NODE"
    const val ACTION_REMOVE_LINK: String = "ACTION_REMOVE_LINK"

    const val INTENT_EXTRA_KEY_PLACEHOLDER: String = "placeholder"
    const val INTENT_EXTRA_KEY_HANDLE: String = "HANDLE"
    const val INTENT_EXTRA_KEY_FILE_NAME: String = "FILENAME"
    const val INTENT_EXTRA_KEY_SCREEN_POSITION: String = "screenPosition"
    const val INTENT_EXTRA_KEY_ADAPTER_TYPE: String = "adapterType"
    const val INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE: String = "mediaQueueTitle"
    const val INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE: String = "videoCollectionTitle"
    const val INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID: String = "videoCollectionId"
    const val INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM: String = "videoAddToAlbum"
    const val INTENT_EXTRA_KEY_VIEWER_FROM: String = "viewerFrom"
    const val INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE: String = "fromDownloadService"
    const val INTENT_EXTRA_KEY_INSIDE: String = "inside"
    const val INTENT_EXTRA_KEY_MAIL: String = "mail"
    const val INTENT_EXTRA_KEY_APP: String = "APP"
    const val INTENT_EXTRA_KEY_IS_FOLDER_LINK: String = "isFolderLink"
    const val INTENT_EXTRA_KEY_ORDER_GET_CHILDREN: String = "orderGetChildren"
    const val INTENT_EXTRA_KEY_PARENT_NODE_HANDLE: String = "parentNodeHandle"
    const val INTENT_EXTRA_KEY_PARENT_ID: String = "parentId"
    const val INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH: String = "handlesNodesSearch"
    const val INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY: String = "offlinePathDirectory"
    const val INTENT_EXTRA_KEY_PATH: String = "path"
    const val INTENT_EXTRA_KEY_PATH_NAVIGATION: String = "pathNavigation"
    const val INTENT_EXTRA_KEY_IS_PLAYLIST: String = "IS_PLAYLIST"
    const val INTENT_EXTRA_KEY_REBUILD_PLAYLIST: String = "REBUILD_PLAYLIST"
    const val INTENT_EXTRA_KEY_FROM: String = "from"
    const val INTENT_EXTRA_KEY_COPY_FROM: String = "COPY_FROM"
    const val INTENT_EXTRA_KEY_IMPORT_CHAT: String = "HANDLES_IMPORT_CHAT"
    const val INTENT_EXTRA_KEY_MOVE_FROM: String = "MOVE_FROM"
    const val INTENT_EXTRA_KEY_MOVE_HANDLES: String = "MOVE_HANDLES"
    const val INTENT_EXTRA_KEY_MOVE_TO: String = "MOVE_TO"
    const val INTENT_EXTRA_KEY_COPY_HANDLES: String = "COPY_HANDLES"
    const val INTENT_EXTRA_KEY_COPY_TO: String = "COPY_TO"
    const val INTENT_EXTRA_KEY_IMPORT_TO: String = "IMPORT_TO"
    const val INTENT_EXTRA_KEY_CONTACT_EMAIL: String = "contactEmail"
    const val INTENT_EXTRA_KEY_LOCATION_FILE_INFO: String = "locationFileInfo"
    const val INTENT_EXTRA_KEY_OFFLINE_ADAPTER: String = "offline_adapter"
    const val INTENT_EXTRA_KEY_PARENT_HANDLE: String = "PARENT_HANDLE"
    const val INTENT_EXTRA_KEY_FRAGMENT_HANDLE: String = "fragmentHandle"
    const val INTENT_EXTRA_KEY_FIRST_LEVEL: String = "firstLevel"
    const val INTENT_EXTRA_KEY_CHAT_ID: String = "chatId"
    const val INTENT_EXTRA_KEY_MAX_USER: String = "max_user"
    const val INTENT_EXTRA_IS_OFFLINE_PATH: String = "IS_OFFLINE_PATH"
    const val INTENT_EXTRA_WARNING_MESSAGE: String = "WARNING_MESSAGE"
    const val INTENT_EXTRA_KEY_MSG_ID: String = "msgId"
    const val INTENT_EXTRA_KEY_CONTACT_TYPE: String = "contactType"
    const val INTENT_EXTRA_KEY_CHAT: String = "chat"
    const val INTENT_EXTRA_KEY_TOOL_BAR_TITLE: String = "aBtitle"
    const val INTENT_EXTRA_IS_FROM_MEETING: String = "extra_is_from_meeting"
    const val INTENT_EXTRA_COLLISION_RESULTS: String = "INTENT_EXTRA_COLLISION_RESULTS"
    const val INTENT_EXTRA_SINGLE_COLLISION_RESULT: String = "INTENT_EXTRA_SINGLE_COLLISION_RESULT"
    const val INTENT_EXTRA_KEY_CONTACTS_SELECTED: String = "INTENT_EXTRA_KEY_CONTACTS_SELECTED"
    const val INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT: String =
        "INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT"

    const val CONTACT_FILE_ADAPTER: Int = 2001
    const val OFFLINE_ADAPTER: Int = 2004
    const val FOLDER_LINK_ADAPTER: Int = 2005
    const val SEARCH_ADAPTER: Int = 2006
    const val PHOTO_SYNC_ADAPTER: Int = 2007
    const val ZIP_ADAPTER: Int = 2008
    const val INCOMING_SHARES_PROVIDER_ADAPTER: Int = 2016
    const val CLOUD_DRIVE_PROVIDER_ADAPTER: Int = 2017
    const val SEARCH_BY_ADAPTER: Int = 2018
    const val FILE_LINK_ADAPTER: Int = 2019
    const val FROM_CHAT: Int = 2020
    const val CONTACT_SHARED_FOLDER_ADAPTER: Int = 2021
    const val RECENTS_ADAPTER: Int = 2024
    const val VIDEO_BROWSE_ADAPTER: Int = 2032
    const val RECENTS_BUCKET_ADAPTER: Int = 2034
    const val VERSIONS_ADAPTER: Int = 2035
    const val FROM_IMAGE_VIEWER: Int = 2036
    const val FROM_MEDIA_DISCOVERY: Int = 2040
    const val FROM_ALBUM_SHARING: Int = 2041
    const val VIEWER_FROM_RECENTS_BUCKET: Int = 8
    const val VIEWER_FROM_CONTACT_FILE_LIST: Int = 11
    const val VIEWER_FROM_ZIP_BROWSER: Int = 13
    const val VIEWER_FROM_FILE_BROWSER: Int = 14
    const val VIEWER_FROM_BACKUPS: Int = 15
    const val VIEWER_FROM_FILE_VERSIONS: Int = 18

    const val NOTIFICATIONS_ENABLED: String = "NOTIFICATIONS_ENABLED"
    const val NOTIFICATIONS_30_MINUTES: String = "NOTIFICATIONS_30_MINUTES"
    const val NOTIFICATIONS_1_HOUR: String = "NOTIFICATIONS_1_HOUR"
    const val NOTIFICATIONS_6_HOURS: String = "NOTIFICATIONS_6_HOURS"
    const val NOTIFICATIONS_24_HOURS: String = "NOTIFICATIONS_24_HOURS"
    const val NOTIFICATIONS_DISABLED_X_TIME: String = "NOTIFICATIONS_DISABLED_X_TIME"
    const val NOTIFICATIONS_DISABLED: String = "NOTIFICATIONS_DISABLED"
    const val NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING: String =
        "NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING"
    const val NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING: String =
        "NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING"

    const val CONTACT_TYPE_MEGA: Int = 0
    const val CONTACT_TYPE_DEVICE: Int = 1
    const val CONTACT_TYPE_BOTH: Int = 2

    const val DEVICE_ANDROID: Int = 1

    const val NOTIFICATION_CAMERA_UPLOADS: Int = 3
    const val NOTIFICATION_PUSH_CLOUD_DRIVE: Int = 7
    const val NOTIFICATION_GENERAL_PUSH_CHAT: Int = 8
    const val NOTIFICATION_SUMMARY_INCOMING_CONTACT: Int = 9
    const val NOTIFICATION_CALL_IN_PROGRESS: Int = 11
    const val NOTIFICATION_MISSED_CALL: Int = 12
    const val NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT: Int = 13
    const val NOTIFICATION_STORAGE_OVER_QUOTA: Int = 14
    const val NOTIFICATION_NO_WIFI_CONNECTION: Int = 15
    const val NOTIFICATION_NO_NETWORK_CONNECTION: Int = 16
    const val NOTIFICATION_NOT_ENOUGH_STORAGE: Int = 17
    const val NOTIFICATION_VIDEO_COMPRESSION: Int = 18
    const val NOTIFICATION_CAMERA_UPLOADS_PRIMARY_FOLDER_UNAVAILABLE: Int = 19
    const val NOTIFICATION_CAMERA_UPLOADS_SECONDARY_FOLDER_UNAVAILABLE: Int = 20
    const val NOTIFICATION_COMPRESSION_ERROR: Int = 21

    const val NOTIFICATION_CHANNEL_DOWNLOAD_ID: String = "DownloadServiceNotification"
    const val NOTIFICATION_CHANNEL_DOWNLOAD_NAME: String = "MEGA Download"
    const val NOTIFICATION_CHANNEL_UPLOAD_ID: String = "UploadServiceNotification"
    const val NOTIFICATION_CHANNEL_UPLOAD_NAME: String = "MEGA File Upload"
    const val NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID: String = "CameraUploadsServiceNotification"
    const val NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME: String = "MEGA Camera Uploads"
    const val NOTIFICATION_CHANNEL_CHAT_ID: String = "ChatNotification"
    const val NOTIFICATION_CHANNEL_CHAT_NAME: String = "MEGA Chat"
    const val NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2: String = "ChatSummaryNotificationV2"
    const val NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME: String = "MEGA Chat Summary"
    const val NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID: String =
        "ChatSummaryNotificationNoVibrate"
    const val NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME: String =
        "MEGA Chat Summary (no vibration)"
    const val NOTIFICATION_CHANNEL_IN_PROGRESS_MISSED_CALLS_ID: String =
        "InProgressMissedCallNotification"
    const val NOTIFICATION_CHANNEL_IN_PROGRESS_MISSED_CALLS_NAME: String =
        "MEGA In Progress and Missed Calls"
    const val NOTIFICATION_CHANNEL_INCOMING_CALLS_ID: String = "ChatIncomingCallNotification"
    const val NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID: String =
        "ChatIncomingCallNotificationNoVibrate"
    const val NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_NAME: String =
        "MEGA Incoming Calls (no vibration)"
    const val NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME: String = "MEGA Incoming Calls"
    const val NOTIFICATION_CHANNEL_CONTACTS_ID: String = "ContactNotification"
    const val NOTIFICATION_CHANNEL_CONTACTS_NAME: String = "MEGA Contact"
    const val NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_ID: String = "ContactSummaryNotification"
    const val NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_NAME: String = "MEGA Contact Summary"
    const val NOTIFICATION_CHANNEL_CLOUDDRIVE_ID: String = "CloudDriveNotification"
    const val NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME: String = "MEGA Cloud Drive"
    const val NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID: String = "ChatUploadServiceNotification"
    const val NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME: String = "MEGA Chat Upload"
    const val NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID: String = "AudioPlayerNotification"
    const val NOTIFICATION_CHANNEL_PROMO_ID: String = "PromoNotification"
    const val NOTIFICATION_CHANNEL_PROMO_NAME: String = "MEGA Promotions"
    const val CHAT_FOLDER: String = "My chat files"

    const val AUTHORITY_STRING_FILE_PROVIDER: String =
        BuildConfig.APPLICATION_ID + ".providers.fileprovider"
    const val TYPE_TEXT_PLAIN: String = "text/plain"

    const val TYPE_LEFT: Int = -1
    const val TYPE_JOIN: Int = 1
    const val MAIL_ANDROID: String = "androidfeedback@mega.io"
    const val MAIL_SUPPORT: String = "support@mega.io"

    //link for introduction end to end encryption
    const val URL_E2EE: String = "https://mega.io/security"

    const val MIN_ITEMS_SCROLLBAR: Int = 30
    const val MIN_ITEMS_SCROLLBAR_CONTACT: Int = 20

    const val MAX_AUTOAWAY_TIMEOUT: Int = 1457 //in minute, the max value supported by SDK

    const val AVATAR_PRIMARY_COLOR: String = "AVATAR_PRIMARY_COLOR"
    const val AVATAR_GROUP_CHAT_COLOR: String = "AVATAR_GROUP_CHAT_COLOR"
    const val AVATAR_PHONE_COLOR: String = "AVATAR_PHONE_COLOR"

    /**
     * A phone number pattern, which length should be in 5-22, and the beginning can have a '+'.
     */
    val PHONE_NUMBER_REGEX: Pattern = Pattern.compile("^[+]?[0-9]{5,22}$")

    @JvmField
    val EMAIL_ADDRESS
            : Pattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\&\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    /**
     * A node name must not contain these characters.
     */
    val NODE_NAME_REGEX: Pattern = Pattern.compile("[*|\\?:\"<>\\\\\\\\/]")

    const val FROM_INCOMING_SHARES: Int = 140
    const val FROM_BACKUPS: Int = 150

    const val SNACKBAR_TYPE: Int = 0
    const val MESSAGE_SNACKBAR_TYPE: Int = 1
    const val MUTE_NOTIFICATIONS_SNACKBAR_TYPE: Int = 2
    const val PERMISSIONS_TYPE: Int = 4
    const val DISMISS_ACTION_SNACKBAR: Int = 6
    const val OPEN_FILE_SNACKBAR_TYPE: Int = 7

    const val NOT_CALL_PERMISSIONS_SNACKBAR_TYPE: Int = 10

    const val HEADER_VIEW_TYPE: Int = 0
    const val ITEM_VIEW_TYPE: Int = 1
    const val ITEM_PROGRESS: Int = 2
    const val ITEM_PLACEHOLDER_TYPE: Int = 3

    const val SCROLLING_UP_DIRECTION: Int = -1
    const val REQUIRE_PASSCODE_INVALID: Int = -1

    const val IS_NODE_INCOMING: String = "isNodeIncoming"
    const val CONTACT_HANDLE: String = "contactHandle"
    const val SHOW_SNACKBAR: String = "SHOW_SNACKBAR"
    const val CHAT_ID: String = "CHAT_ID"
    const val MESSAGE_ID: String = "messageId"
    const val CHAT_ID_OF_CURRENT_CALL: String = "chatHandleInProgress"
    const val CHAT_ID_OF_INCOMING_CALL: String = "chatHandleToAnswer"
    const val PEER_ID: String = "peerId"
    const val CLIENT_ID: String = "clientId"
    const val SELECTED_CONTACTS: String = "SELECTED_CONTACTS"
    const val NODE_HANDLES: String = "NODE_HANDLES"
    const val NAME: String = "name"
    const val HANDLE: String = "handle"
    const val HANDLE_LIST: String = "HANDLE_LIST"
    const val EMAIL: String = "email"
    const val UNKNOWN_USER_NAME_AVATAR: String = "unknown"
    const val VISIBLE_FRAGMENT: String = "VISIBLE_FRAGMENT"
    const val LAUNCH_INTENT: String = "LAUNCH_INTENT"
    const val SELECTED_CHATS: String = "SELECTED_CHATS"
    const val SELECTED_USERS: String = "SELECTED_USERS"
    const val ID_MESSAGES: String = "ID_MESSAGES"
    const val ID_CHAT_FROM: String = "ID_CHAT_FROM"
    const val USER_HANDLES: String = "USER_HANDLES"
    const val ID_MSG: String = "ID_MSG"
    const val IS_OVER_QUOTA: String = "IS_OVERQUOTA"
    const val URL_FILE_LINK: String = "URL_FILE_LINK"
    const val OPEN_SCAN_QR: String = "OPEN_SCAN_QR"
    const val INVITE_CONTACT: String = "INVITE_CONTACT"
    const val TYPE_CALL_PERMISSION: String = "TYPE_CALL_PERMISSION"
    const val VOLUME_CHANGED_ACTION: String = "android.media.VOLUME_CHANGED_ACTION"
    const val EXTRA_VOLUME_STREAM_VALUE: String = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    const val EXTRA_VOLUME_STREAM_TYPE: String = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    const val COPIED_TEXT_LABEL: String = "Copied Text"
    const val IS_FLOATING_WINDOW: String = "IS_FLOATING_WINDOW"
    const val SCHEDULED_MEETING_ID: String = "SCHEDULED_MEETING_ID"
    const val SCHEDULED_MEETING_CREATED: String = "SCHEDULED_MEETING_CREATED"
    const val INVALID_POSITION: Int = -1
    const val INVALID_OPTION: String = "-1"
    const val INVALID_TYPE_PERMISSIONS: Int = -1
    const val INVALID_VOLUME: Int = -1
    const val INVALID_DIMENSION: Int = -1
    const val INVALID_CALL_STATUS: Int = -1
    const val INVALID_CALL: Int = -1

    const val EXTRA_MESSAGE: String = "EXTRA_MESSAGE"

    const val MAX_WIDTH_CONTACT_NAME_LAND: Int = 450
    const val MAX_WIDTH_CONTACT_NAME_PORT: Int = 200
    const val EMOJI_SIZE: Int = 20
    const val MAX_ALLOWED_CHARACTERS_AND_EMOJIS: Int = 28
    const val MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND: Int = 350
    const val MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT: Int = 200
    const val MAX_WIDTH_ADD_CONTACTS: Int = 60
    const val AVATAR_SIZE_CALLS: Int = 50
    const val AVATAR_SIZE_GRID: Int = 75
    const val AVATAR_SIZE: Int = 150
    const val MEETING_BOTTOM_MARGIN: Float = 40f
    const val MEETING_BOTTOM_MARGIN_WITH_KEYBOARD: Float = 10f
    const val MIN_MEETING_HEIGHT_CHANGE: Float = 200f

    //Thumbnail dimens
    const val THUMB_CORNER_RADIUS_DP: Float = 4f
    const val THUMB_SIZE_DP: Int = 40
    const val THUMB_MARGIN_DP: Int = 16
    const val ICON_SIZE_DP: Int = 48
    const val ICON_MARGIN_DP: Int = 12

    const val ALPHA_VIEW_DISABLED: Float = 0.3f
    const val ALPHA_VIEW_ENABLED: Float = 1.0f

    const val AUDIO_MANAGER_PLAY_VOICE_CLIP: Int = 0
    const val AUDIO_MANAGER_CALL_RINGING: Int = 1
    const val AUDIO_MANAGER_CALL_IN_PROGRESS: Int = 2
    const val AUDIO_MANAGER_CALL_OUTGOING: Int = 3
    const val AUDIO_MANAGER_CREATING_JOINING_MEETING: Int = 4

    const val CHECK_LINK_TYPE_UNKNOWN_LINK: Int = 0
    const val CHECK_LINK_TYPE_CHAT_LINK: Int = 1
    const val CHECK_LINK_TYPE_MEETING_LINK: Int = 2

    val SEPARATOR: String = File.separator

    /**
     * This Regex Pattern will check for the existence of:
     * 1. Domain with HTTPS protocol
     * 2. Followed by either: mega.co.nz, mega.nz, mega.io, megaad.nz, mega.app
     * 3. No words are allowed after the domain name, for example; [https://mega.co.nzxxx](https://mega.co.nzxxx) is not allowed
     * 4. Backslashes (/) or Question Mark (?) are allowed to allow path and query parameters after the MEGA domain, for example; [https://mega.nz/home](https://mega.nz/home)
     * 5. Any characters after Backslashes (/) or Question Mark (?) are allowed, except At Sign(@)
     * 6. Pure domain is allowed. For example: [https://mega.nz](https://mega.nz)
     */
    val MEGA_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz|\\.app)(\\/|\\?)[^@]*$",
        "^https://mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz|\\.app)$",
        "^https://([a-z0-9]+\\.)+mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz|\\.app)(\\/|\\?)[^@]*$"
    )

    val FILE_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#!.+$",
        "^https://mega\\.nz/.*#!.+$",
        "^https://mega\\.app/.*#!.+$",
        "^https://mega\\.co\\.nz/file/.+$",
        "^https://mega\\.nz/file/.+$",
        "^https://mega\\.app/file/.+$",
    )

    val CONFIRMATION_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#confirm.+$",
        "^https://mega\\.co\\.nz/.*confirm.+$",
        "^https://mega\\.nz/.*#confirm.+$",
        "^https://mega\\.nz/.*confirm.+$",
        "^https://mega\\.app/.*#confirm.+$",
        "^https://mega\\.app/.*confirm.+$"
    )

    val FOLDER_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#F!.+$",
        "^https://mega\\.nz/.*#F!.+$",
        "^https://mega\\.app/.*#F!.+$",
        "^https://mega\\.co\\.nz/folder/.+$",
        "^https://mega\\.nz/folder/.+$",
        "^https://mega\\.app/folder/.+$"
    )

    val CHAT_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*chat/.+$",
        "^https://mega\\.nz/.*chat/.+$",
        "^https://mega\\.app/.*chat/.+$"
    )

    val PASSWORD_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#P!.+$",
        "^https://mega\\.nz/.*#P!.+$",
        "^https://mega\\.app/.*#P!.+$"
    )

    val ACCOUNT_INVITATION_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#newsignup.+$",
        "^https://mega\\.co\\.nz/.*newsignup.+$",
        "^https://mega\\.nz/.*#newsignup.+$",
        "^https://mega\\.nz/.*newsignup.+$",
        "^https://mega\\.app/.*#newsignup.+$",
        "^https://mega\\.app/.*newsignup.+$"
    )

    val EXPORT_MASTER_KEY_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#backup",
        "^https://mega\\.nz/.*#backup",
        "^https://mega\\.app/.*#backup"
    )

    val NEW_MESSAGE_CHAT_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#fm/chat",
        "^https://mega\\.co\\.nz/.*fm/chat",
        "^https://mega\\.nz/.*#fm/chat",
        "^https://mega\\.nz/.*fm/chat",
        "^https://mega\\.app/.*#fm/chat",
        "^https://mega\\.app/.*fm/chat"
    )

    val CANCEL_ACCOUNT_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#cancel.+$",
        "^https://mega\\.co\\.nz/.*cancel.+$",
        "^https://mega\\.nz/.*#cancel.+$",
        "^https://mega\\.nz/.*cancel.+$",
        "^https://mega\\.app/.*#cancel.+$",
        "^https://mega\\.app/.*cancel.+$"
    )

    val VERIFY_CHANGE_MAIL_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#verify.+$",
        "^https://mega\\.co\\.nz/.*verify.+$",
        "^https://mega\\.nz/.*#verify.+$",
        "^https://mega\\.nz/.*verify.+$",
        "^https://mega\\.app/.*#verify.+$",
        "^https://mega\\.app/.*verify.+$"
    )

    val RESET_PASSWORD_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#recover.+$",
        "^https://mega\\.co\\.nz/.*recover.+$",
        "^https://mega\\.nz/.*#recover.+$",
        "^https://mega\\.nz/.*recover.+$",
        "^https://mega\\.app/.*#recover.+$",
        "^https://mega\\.app/.*recover.+$"
    )

    val PENDING_CONTACTS_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#fm/ipc",
        "^https://mega\\.co\\.nz/.*fm/ipc",
        "^https://mega\\.nz/.*#fm/ipc",
        "^https://mega\\.nz/.*fm/ipc",
        "^https://mega\\.app/.*#fm/ipc",
        "^https://mega\\.app/.*fm/ipc"
    )

    val HANDLE_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#.+$",
        "^https://mega\\.nz/.*#.+$",
        "^https://mega\\.app/.*#.+$"
    )

    val CONTACT_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/C!.+$",
        "^https://mega\\.nz/.*C!.+$",
        "^https://mega\\.app/.*C!.+$"
    )

    val MEGA_DROP_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*megadrop/.+$",
        "^https://mega\\.nz/.*megadrop/.+$",
        "^https://mega\\.app/.*megadrop/.+$"
    )

    val MEGA_FILE_REQUEST_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*filerequest/.+$",
        "^https://mega\\.nz/.*filerequest/.+$",
        "^https://mega\\.app/.*filerequest/.+$"
    )

    val MEGA_BLOG_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#blog",
        "^https://mega\\.nz/.*#blog",
        "^https://mega\\.nz/.*blog",
        "^https://mega\\.co\\.nz/.*#blog.+$",
        "^https://mega\\.nz/.*#blog.+$",
        "^https://mega\\.nz/.*blog.+$",
        "^https://mega\\.app/.*#blog",
        "^https://mega\\.app/.*blog",
        "^https://mega\\.app/.*#blog.+$",
        "^https://mega\\.app/.*blog.+$"
    )

    val REVERT_CHANGE_PASSWORD_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/.*#pwr.+$",
        "^https://mega\\.co\\.nz/.*pwr.+$",
        "^https://mega\\.nz/.*#pwr.+$",
        "^https://mega\\.nz/.*pwr.+$",
        "^https://mega\\.app/.*#pwr.+$",
        "^https://mega\\.app/.*pwr.+$"
    )

    val EMAIL_VERIFY_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/#emailverify.+$",
        "^https://mega\\.co\\.nz/emailverify.+$",
        "^https://mega\\.nz/#emailverify.+$",
        "^https://mega\\.nz/emailverify.+$",
        "^https://mega\\.app/#emailverify.+$",
        "^https://mega\\.app/emailverify.+$"
    )

    val WEB_SESSION_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/#sitetransfer!.+$",
        "^https://mega\\.nz/#sitetransfer!.+$",
        "^https://mega\\.app/#sitetransfer!.+$"
    )

    val BUSINESS_INVITE_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.co\\.nz/#businessinvite.+$",
        "^https://mega\\.co\\.nz/businessinvite.+$",
        "^https://mega\\.nz/#businessinvite.+$",
        "^https://mega\\.nz/businessinvite.+$",
        "^https://mega\\.app/#businessinvite.+$",
        "^https://mega\\.app/businessinvite.+$"
    )

    val ALBUM_LINK_REGEX_ARRAY: Array<String> = arrayOf(
        "^https://mega\\.nz/collection/.+$",
        "^https://mega\\.app/collection/.+$",
    )


    const val INVALID_VALUE: Int = -1

    const val INVALID_SIZE: Long = -1

    const val LOCATION_INDEX_LEFT: Int = 0
    const val LOCATION_INDEX_TOP: Int = 1
    const val LOCATION_INDEX_WIDTH: Int = 2
    const val LOCATION_INDEX_HEIGHT: Int = 3

    const val OFFLINE_ROOT: String = "/"

    const val AUDIO_PLAYER_TRACK_NAME_FADE_DURATION_MS: Long = 200
    const val AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS: Long = 3000
    const val MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS: Long = 300
    const val KEY_IS_SHOWED_WARNING_MESSAGE: String = "is_showed_meeting_warning_message_"

    const val STRING_SEPARATOR: String = " · "

    const val ORDER_CLOUD: Int = 0
    const val ORDER_OTHERS: Int = 1
    const val ORDER_CAMERA: Int = 2
    const val ORDER_OFFLINE: Int = 3
    const val ORDER_FAVOURITES: Int = 4
    const val ORDER_VIDEO_PLAYLIST: Int = 5
    const val ORDER_OUTGOING_SHARES: Int = 6

    const val MAX_WIDTH_APPBAR_LAND: Float = 400f
    const val MAX_WIDTH_APPBAR_PORT: Float = 200f

    const val ANIMATION_DURATION: Long = 400

    const val URL_INDICATOR: String = "URL="

    /**
     * The param type returned by checkChatLink denoting the link is for a meeting room
     */
    const val LINK_IS_FOR_MEETING: Int = 1

    const val MEETING_NAME_MARGIN_TOP: Float = 16f

    const val NAME_CHANGE: Int = 0
    const val AVATAR_CHANGE: Int = 1

    const val FIRST_NAVIGATION_LEVEL: Int = 0

    const val LONG_SNACKBAR_DURATION: Long = 2750

    const val CANNOT_OPEN_FILE_SHOWN: String = "CANNOT_OPEN_FILE_SHOWN"

    const val MAX_TITLE_SIZE: Int = 30

    const val MAX_DESCRIPTION_SIZE: Int = 3000


    private const val PACKAGE_NAME = "id=mega.privacy.android.app"
    const val MARKET_URI: String = "market://details?$PACKAGE_NAME"
    const val PLAY_STORE_URI: String = "https://play.google.com/store/apps/details?$PACKAGE_NAME"

    const val MEGA_TRANSFER_IT_URL: String = "https://transfer.it"
    const val MEGA_VPN_PACKAGE_NAME: String = "mega.vpn.android.app"
    const val MEGA_PASS_PACKAGE_NAME: String = "mega.pwm.android.app"
}
