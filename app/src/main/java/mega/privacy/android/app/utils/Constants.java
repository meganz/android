package mega.privacy.android.app.utils;

import static mega.privacy.android.app.BuildConfig.APPLICATION_ID;

import java.io.File;
import java.util.regex.Pattern;

public class Constants {

    public static final String PIN_4 = "4";
    public static final String PIN_6 = "6";
    public static final String PIN_ALPHANUMERIC = "alphanumeric";

    public static final int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels

    public static final int REQUEST_CODE_SELECT_FOLDER_TO_MOVE = 1001;
    public static final int REQUEST_CODE_SELECT_FOLDER_TO_COPY = 1002;
    public static final int REQUEST_CODE_REFRESH = 1005;
    public static final int REQUEST_CODE_SELECT_IMPORT_FOLDER = 1007;
    public static final int REQUEST_CODE_SELECT_CONTACT = 1009;
    public static final int TAKE_PHOTO_CODE = 1010;
    public static final int TAKE_PICTURE_PROFILE_CODE = 1015;
    public static final int REQUEST_CREATE_CHAT = 1018;
    public static final int REQUEST_ADD_PARTICIPANTS = 1019;
    public static final int REQUEST_CODE_SELECT_CHAT = 1025;
    public static final int REQUEST_CODE_FILE_INFO = 1027;
    public static final int REQUEST_CODE_REFRESH_API_SERVER = 1028;
    public static final int REQUEST_CODE_GET_FOLDER = 1038;
    public static final int REQUEST_CODE_GET_FOLDER_CONTENT = 1039;

    public static final String ACTION_REFRESH_AFTER_BLOCKED = "ACTION_REFRESH_AFTER_BLOCKED";
    public static final String ACTION_REFRESH = "ACTION_REFRESH";
    public static final String ACTION_REFRESH_API_SERVER = "ACTION_REFRESH_API_SERVER";
    public static final String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
    public static final String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";

    public static final String ACTION_FORWARD_MESSAGES = "ACTION_FORWARD_MESSAGES";
    public static final String ACTION_OPEN_QR = "ACTION_OPEN_QR";
    public static final String ACTION_TAKE_PICTURE = "ACTION_TAKE_PICTURE";
    public static final String ACTION_TAKE_PROFILE_PICTURE = "ACTION_TAKE_PROFILE_PICTURE";
    public static final String ACTION_PREVIEW_GIPHY = "ACTION_PREVIEW_GIPHY";

    public static final String SHOW_REPEATED_UPLOAD = "SHOW_REPEATED_UPLOAD";

    public static final String EXTRA_SERIALIZE_STRING = "SERIALIZE_STRING";

    public static final String EXTRA_USER_NICKNAME = "EXTRA_USER_NICKNAME";

    public static final String EXTRA_ACTION_RESULT = "EXTRA_ACTION_RESULT";

    public static final String FROM_HOME_PAGE = "FROM_HOME_PAGE";

    public static final String RESULT = "RESULT";
    public static final String ACCOUNT_BLOCKED_STRING = "ACCOUNT_BLOCKED_STRING";
    public static final String ACCOUNT_BLOCKED_TYPE = "ACCOUNT_BLOCKED_TYPE";
    public static final String ACTION_SHOW_WARNING_ACCOUNT_BLOCKED = "ACTION_SHOW_WARNING_ACCOUNT_BLOCKED";

    public static final String EXTRA_STORAGE_STATE = "STORAGE_STATE";

    public static final String EXTRA_MOVE_TO_CHAT_SECTION = "EXTRA_MOVE_TO_CHAT_SECTION";

    //MultipleRequestListener options
    public static final int MULTIPLE_SEND_RUBBISH = 1;
    public static final int MULTIPLE_LEAVE_SHARE = 8;

    public static final int CANCEL_ACCOUNT_2FA = 4000;
    public static final int CHANGE_MAIL_2FA = 4001;
    public static final int DISABLE_2FA = 4002;
    public static final int CHANGE_PASSWORD_2FA = 4003;

    public static final int TOUR_FRAGMENT = 6000;
    public static final int LOGIN_FRAGMENT = 6001;
    public static final int CONFIRM_EMAIL_FRAGMENT = 6002;
    public static final int CREATE_ACCOUNT_FRAGMENT = 604;


    public static final int UPDATE_ACCOUNT_DETAILS = 9003;

    public static final int REQUEST_WRITE_STORAGE = 1;
    public static final int REQUEST_CAMERA = 2;
    public static final int REQUEST_READ_CONTACTS = 3;
    public static final int REQUEST_RECORD_AUDIO = 4;

    public static final int REQUEST_READ_WRITE_STORAGE = 9;

    public static final int IMPORT_ONLY_OPTION = 0;
    public static final int IMPORT_TO_SHARE_OPTION = 2;

    public static final int FREE = 0;
    public static final int PRO_I = 1;
    public static final int PRO_II = 2;
    public static final int PRO_III = 3;
    public static final int PRO_LITE = 4;
    public static final int BUSINESS = 100;
    public static final int PRO_FLEXI = 101;

    public static final long DISABLED_RETENTION_TIME = 0;
    public static final long SECONDS_IN_MINUTE = 60;
    public static final int SECONDS_IN_HOUR = 3600;
    public static final int SECONDS_IN_DAY = SECONDS_IN_HOUR * 24;
    public static final int SECONDS_IN_WEEK = SECONDS_IN_DAY * 7;
    public static final int SECONDS_IN_MONTH_30 = SECONDS_IN_DAY * 30;
    public static final int SECONDS_IN_YEAR = SECONDS_IN_DAY * 365;

    public static final long SECONDS_TO_WAIT_ALONE_ON_THE_CALL = 2 * SECONDS_IN_MINUTE;
    public static final long SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL = 5 * SECONDS_IN_MINUTE;

    public static final String SCANNED_CONTACT_BASE_URL = "https://mega.nz/";
    public static final String DISPUTE_URL = "https://mega.io/dispute";
    public static final String TAKEDOWN_URL = "https://mega.io/takedown";
    public static final String TERMS_OF_SERVICE_URL = "https://mega.io/terms";
    public static final String PRIVACY_POLICY_URL = "https://mega.io/privacy";
    public static final String PRICING_PAGE_URL = "https://mega.io/pricing";
    public static final String HELP_CENTRE_HOME_URL = "https://help.mega.io";
    public static final String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
    public static final String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
    public static final String ACTION_IMPORT_LINK_FETCH_NODES = "IMPORT_LINK_FETCH_NODES";
    public static final String ACTION_FILE_EXPLORER_UPLOAD = "FILE_EXPLORER_UPLOAD";
    public static final String ACTION_FILE_PROVIDER = "ACTION_FILE_PROVIDER";
    public static final String ACTION_EXPLORE_ZIP = "EXPLORE_ZIP";
    public static final String EXTRA_PATH_ZIP = "PATH_ZIP";
    public static final String EXTRA_HANDLE_ZIP = "HANDLE_ZIP";
    public static final String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLDER";
    public static final String ACTION_OVERQUOTA_STORAGE = "OVERQUOTA_STORAGE";
    public static final String ACTION_TAKE_SELFIE = "TAKE_SELFIE";
    public static final String ACTION_SHOW_TRANSFERS = "SHOW_TRANSFERS";
    public static final String ACTION_LOCATE_DOWNLOADED_FILE = "LOCATE_DOWNLOADED_FILE";
    public static final String ACTION_EXPORT_MASTER_KEY = "EXPORT_MASTER_KEY";
    public static final String ACTION_OPEN_FOLDER = "OPEN_FOLDER";
    public static final String ACTION_CANCEL_ACCOUNT = "CANCEL_ACCOUNT";
    public static final String ACTION_RESET_PASS = "RESET_PASS";
    public static final String ACTION_RESET_PASS_FROM_LINK = "RESET_PASS_FROM_LINK";
    public static final String ACTION_PASS_CHANGED = "PASS_CHANGED";
    public static final String ACTION_RESET_PASS_FROM_PARK_ACCOUNT = "RESET_PASS_FROM_PARK_ACCOUNT";
    public static final String ACTION_CHANGE_MAIL = "CHANGE_MAIL";
    public static final String ACTION_IPC = "IPC";
    public static final String ACTION_SHOW_MY_ACCOUNT = "ACTION_SHOW_MY_ACCOUNT";
    public static final String ACTION_CHAT_NOTIFICATION_MESSAGE = "ACTION_CHAT_MESSAGE";
    public static final String ACTION_CHAT_SUMMARY = "ACTION_CHAT_SUMMARY";
    public static final String ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION = "ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION";
    public static final String ACTION_OPEN_HANDLE_NODE = "ACTION_OPEN_HANDLE_NODE";
    public static final String ACTION_OPEN_FILE_LINK_ROOTNODES_NULL = "ACTION_OPEN_FILE_LINK_ROOTNODES_NULL";
    public static final String ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL = "ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL";
    public static final String ACTION_SHOW_SETTINGS = "ACTION_SHOW_SETTINGS";
    public static final String ACTION_SHOW_SETTINGS_STORAGE = "ACTION_SHOW_SETTINGS_STORAGE";
    public static final String ACTION_PRE_OVERQUOTA_STORAGE = "PRE_OVERQUOTA_STORAGE";
    public static final String ACTION_LOG_IN = "ACTION_LOG_IN";
    public static final String ACTION_OPEN_DEVICE_CENTER = "ACTION_OPEN_DEVICE_CENTER";
    public static final String ACTION_OPEN_SYNC_MEGA_FOLDER = "ACTION_OPEN_SYNC_MEGA_FOLDER";
    public static final String ACTION_SHOW_WARNING = "ACTION_SHOW_WARNING";
    public static final String OPENED_FROM_CHAT = "OPENED_FROM_CHAT";
    public static final String ACTION_OPEN_CHAT_LINK = "OPEN_CHAT_LINK";
    public static final String ACTION_JOIN_OPEN_CHAT_LINK = "JOIN_OPEN_CHAT_LINK";
    public static final String ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES";
    public static final String ACTION_SHOW_UPGRADE_ACCOUNT = "ACTION_SHOW_UPGRADE_ACCOUNT";
    public static final String ACTION_OPEN_CONTACTS_SECTION = "ACTION_OPEN_CONTACTS_SECTION";

    public static final String ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE = "ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE";

    public static final String ACTION_SHARE_MSG = "ACTION_SHARE_MSG";
    public static final String ACTION_SHARE_NODE = "ACTION_SHARE_NODE";
    public static final String ACTION_REMOVE_LINK = "ACTION_REMOVE_LINK";

    public static final String INTENT_EXTRA_KEY_PLACEHOLDER = "placeholder";
    public static final String INTENT_EXTRA_KEY_HANDLE = "HANDLE";
    public static final String INTENT_EXTRA_KEY_FILE_NAME = "FILENAME";
    public static final String INTENT_EXTRA_KEY_SCREEN_POSITION = "screenPosition";
    public static final String INTENT_EXTRA_KEY_ADAPTER_TYPE = "adapterType";
    public static final String INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE = "mediaQueueTitle";
    public static final String INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE = "videoCollectionTitle";
    public static final String INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID = "videoCollectionId";
    public static final String INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM = "videoAddToAlbum";
    public static final String INTENT_EXTRA_KEY_VIEWER_FROM = "viewerFrom";
    public static final String INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE = "fromDownloadService";
    public static final String INTENT_EXTRA_KEY_INSIDE = "inside";
    public static final String INTENT_EXTRA_KEY_MAIL = "mail";
    public static final String INTENT_EXTRA_KEY_APP = "APP";
    public static final String INTENT_EXTRA_KEY_IS_FOLDER_LINK = "isFolderLink";
    public static final String INTENT_EXTRA_KEY_ORDER_GET_CHILDREN = "orderGetChildren";
    public static final String INTENT_EXTRA_KEY_PARENT_NODE_HANDLE = "parentNodeHandle";
    public static final String INTENT_EXTRA_KEY_PARENT_ID = "parentId";
    public static final String INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH = "handlesNodesSearch";
    public static final String INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY = "offlinePathDirectory";
    public static final String INTENT_EXTRA_KEY_PATH = "path";
    public static final String INTENT_EXTRA_KEY_PATH_NAVIGATION = "pathNavigation";
    public static final String INTENT_EXTRA_KEY_IS_PLAYLIST = "IS_PLAYLIST";
    public static final String INTENT_EXTRA_KEY_REBUILD_PLAYLIST = "REBUILD_PLAYLIST";
    public static final String INTENT_EXTRA_KEY_FROM = "from";
    public static final String INTENT_EXTRA_KEY_COPY_FROM = "COPY_FROM";
    public static final String INTENT_EXTRA_KEY_IMPORT_CHAT = "HANDLES_IMPORT_CHAT";
    public static final String INTENT_EXTRA_KEY_MOVE_FROM = "MOVE_FROM";
    public static final String INTENT_EXTRA_KEY_MOVE_HANDLES = "MOVE_HANDLES";
    public static final String INTENT_EXTRA_KEY_MOVE_TO = "MOVE_TO";
    public static final String INTENT_EXTRA_KEY_COPY_HANDLES = "COPY_HANDLES";
    public static final String INTENT_EXTRA_KEY_COPY_TO = "COPY_TO";
    public static final String INTENT_EXTRA_KEY_IMPORT_TO = "IMPORT_TO";
    public static final String INTENT_EXTRA_KEY_CONTACT_EMAIL = "contactEmail";
    public static final String INTENT_EXTRA_KEY_LOCATION_FILE_INFO = "locationFileInfo";
    public static final String INTENT_EXTRA_KEY_OFFLINE_ADAPTER = "offline_adapter";
    public static final String INTENT_EXTRA_KEY_PARENT_HANDLE = "PARENT_HANDLE";
    public static final String INTENT_EXTRA_KEY_FRAGMENT_HANDLE = "fragmentHandle";
    public static final String INTENT_EXTRA_KEY_FIRST_LEVEL = "firstLevel";
    public static final String INTENT_EXTRA_KEY_CHAT_ID = "chatId";
    public static final String INTENT_EXTRA_KEY_MAX_USER = "max_user";
    public static final String INTENT_EXTRA_IS_OFFLINE_PATH = "IS_OFFLINE_PATH";
    public static final String INTENT_EXTRA_WARNING_MESSAGE = "WARNING_MESSAGE";
    public static final String INTENT_EXTRA_KEY_MSG_ID = "msgId";
    public static final String INTENT_EXTRA_KEY_CONTACT_TYPE = "contactType";
    public static final String INTENT_EXTRA_KEY_CHAT = "chat";
    public static final String INTENT_EXTRA_KEY_TOOL_BAR_TITLE = "aBtitle";
    public static final String INTENT_EXTRA_IS_FROM_MEETING = "extra_is_from_meeting";
    public static final String INTENT_EXTRA_COLLISION_RESULTS = "INTENT_EXTRA_COLLISION_RESULTS";
    public static final String INTENT_EXTRA_SINGLE_COLLISION_RESULT = "INTENT_EXTRA_SINGLE_COLLISION_RESULT";
    public static final String INTENT_EXTRA_KEY_CONTACTS_SELECTED = "INTENT_EXTRA_KEY_CONTACTS_SELECTED";
    public static final String INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT = "INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT";

    public static final int CONTACT_FILE_ADAPTER = 2001;
    public static final int OFFLINE_ADAPTER = 2004;
    public static final int FOLDER_LINK_ADAPTER = 2005;
    public static final int SEARCH_ADAPTER = 2006;
    public static final int PHOTO_SYNC_ADAPTER = 2007;
    public static final int ZIP_ADAPTER = 2008;
    public static final int INCOMING_SHARES_PROVIDER_ADAPTER = 2016;
    public static final int CLOUD_DRIVE_PROVIDER_ADAPTER = 2017;
    public static final int SEARCH_BY_ADAPTER = 2018;
    public static final int FILE_LINK_ADAPTER = 2019;
    public static final int FROM_CHAT = 2020;
    public static final int CONTACT_SHARED_FOLDER_ADAPTER = 2021;
    public static final int RECENTS_ADAPTER = 2024;
    public static final int DOCUMENTS_SEARCH_ADAPTER = 2031;
    public static final int VIDEO_BROWSE_ADAPTER = 2032;
    public static final int RECENTS_BUCKET_ADAPTER = 2034;
    public static final int VERSIONS_ADAPTER = 2035;
    public static final int FROM_IMAGE_VIEWER = 2036;
    public static final int FROM_MEDIA_DISCOVERY = 2040;
    public static final int FROM_ALBUM_SHARING = 2041;
    public static final int VIEWER_FROM_RECETS_BUCKET = 8;
    public static final int VIEWER_FROM_CONTACT_FILE_LIST = 11;
    public static final int VIEWER_FROM_ZIP_BROWSER = 13;
    public static final int VIEWER_FROM_FILE_BROWSER = 14;
    public static final int VIEWER_FROM_BACKUPS = 15;
    public static final int VIEWER_FROM_FILE_VERSIONS = 18;

    public static final String NOTIFICATIONS_ENABLED = "NOTIFICATIONS_ENABLED";
    public static final String NOTIFICATIONS_30_MINUTES = "NOTIFICATIONS_30_MINUTES";
    public static final String NOTIFICATIONS_1_HOUR = "NOTIFICATIONS_1_HOUR";
    public static final String NOTIFICATIONS_6_HOURS = "NOTIFICATIONS_6_HOURS";
    public static final String NOTIFICATIONS_24_HOURS = "NOTIFICATIONS_24_HOURS";
    public static final String NOTIFICATIONS_DISABLED_X_TIME = "NOTIFICATIONS_DISABLED_X_TIME";
    public static final String NOTIFICATIONS_DISABLED = "NOTIFICATIONS_DISABLED";
    public static final String NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING = "NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING";
    public static final String NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING = "NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING";

    public static final int CONTACT_TYPE_MEGA = 0;
    public static final int CONTACT_TYPE_DEVICE = 1;
    public static final int CONTACT_TYPE_BOTH = 2;

    public static final int DEVICE_ANDROID = 1;

    public static final int NOTIFICATION_SUMMARY_CHAT = 0;
    public static final int NOTIFICATION_CAMERA_UPLOADS = 3;
    public static final int NOTIFICATION_PUSH_CLOUD_DRIVE = 7;
    public static final int NOTIFICATION_GENERAL_PUSH_CHAT = 8;
    public static final int NOTIFICATION_SUMMARY_INCOMING_CONTACT = 9;
    public static final int NOTIFICATION_CALL_IN_PROGRESS = 11;
    public static final int NOTIFICATION_MISSED_CALL = 12;
    public static final int NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT = 13;
    public static final int NOTIFICATION_STORAGE_OVERQUOTA = 14;
    public static final int NOTIFICATION_NO_WIFI_CONNECTION = 15;
    public static final int NOTIFICATION_NO_NETWORK_CONNECTION = 16;
    public static final int NOTIFICATION_NOT_ENOUGH_STORAGE = 17;
    public static final int NOTIFICATION_VIDEO_COMPRESSION = 18;
    public static final int NOTIFICATION_CAMERA_UPLOADS_PRIMARY_FOLDER_UNAVAILABLE = 19;
    public static final int NOTIFICATION_CAMERA_UPLOADS_SECONDARY_FOLDER_UNAVAILABLE = 20;
    public static final int NOTIFICATION_COMPRESSION_ERROR = 21;

    public static final String NOTIFICATION_CHANNEL_DOWNLOAD_ID = "DownloadServiceNotification";
    public static final String NOTIFICATION_CHANNEL_DOWNLOAD_NAME = "MEGA Download";
    public static final String NOTIFICATION_CHANNEL_UPLOAD_ID = "UploadServiceNotification";
    public static final String NOTIFICATION_CHANNEL_UPLOAD_NAME = "MEGA File Upload";
    public static final String NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID = "CameraUploadsServiceNotification";
    public static final String NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME = "MEGA Camera Uploads";
    public static final String NOTIFICATION_CHANNEL_CHAT_ID = "ChatNotification";
    public static final String NOTIFICATION_CHANNEL_CHAT_NAME = "MEGA Chat";
    public static final String NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2 = "ChatSummaryNotificationV2";
    public static final String NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME = "MEGA Chat Summary";
    public static final String NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID = "ChatSummaryNotificationNoVibrate";
    public static final String NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME = "MEGA Chat Summary (no vibration)";
    public static final String NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID = "InProgressMissedCallNotification";
    public static final String NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME = "MEGA In Progress and Missed Calls";
    public static final String NOTIFICATION_CHANNEL_INCOMING_CALLS_ID = "ChatIncomingCallNotification";
    public static final String NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID = "ChatIncomingCallNotificationNoVibrate";
    public static final String NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_NAME = "MEGA Incoming Calls (no vibration)";
    public static final String NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME = "MEGA Incoming Calls";
    public static final String NOTIFICATION_CHANNEL_CONTACTS_ID = "ContactNotification";
    public static final String NOTIFICATION_CHANNEL_CONTACTS_NAME = "MEGA Contact";
    public static final String NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_ID = "ContactSummaryNotification";
    public static final String NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_NAME = "MEGA Contact Summary";
    public static final String NOTIFICATION_CHANNEL_CLOUDDRIVE_ID = "CloudDriveNotification";
    public static final String NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME = "MEGA Cloud Drive";
    public static final String NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID = "ChatUploadServiceNotification";
    public static final String NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME = "MEGA Chat Upload";
    public static final String NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID = "AudioPlayerNotification";
    public static final String NOTIFICATION_CHANNEL_PROMO_ID = "PromoNotification";
    public static final String NOTIFICATION_CHANNEL_PROMO_NAME = "MEGA Promotions";
    public static final String CHAT_FOLDER = "My chat files";
    public static final String AUTHORITY_STRING_FILE_PROVIDER = APPLICATION_ID + ".providers.fileprovider";
    public static final String TYPE_TEXT_PLAIN = "text/plain";

    public static int TYPE_LEFT = -1;
    public static int TYPE_JOIN = 1;
    public static final String MAIL_ANDROID = "androidfeedback@mega.nz";
    public static final String MAIL_SUPPORT = "support@mega.nz";

    //link for introduction end to end encryption
    public static final String URL_E2EE = "https://mega.io/security";

    public static final int MIN_ITEMS_SCROLLBAR = 30;
    public static final int MIN_ITEMS_SCROLLBAR_CONTACT = 20;

    public static final int MAX_AUTOAWAY_TIMEOUT = 1457; //in minute, the max value supported by SDK

    public static final String AVATAR_PRIMARY_COLOR = "AVATAR_PRIMARY_COLOR";
    public static final String AVATAR_GROUP_CHAT_COLOR = "AVATAR_GROUP_CHAT_COLOR";
    public static final String AVATAR_PHONE_COLOR = "AVATAR_PHONE_COLOR";

    /**
     * A phone number pattern, which length should be in 5-22, and the beginning can have a '+'.
     */
    public static final Pattern PHONE_NUMBER_REGEX = Pattern.compile("^[+]?[0-9]{5,22}$");

    public static final Pattern EMAIL_ADDRESS
            = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\&\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    /**
     * A node name must not contain these characters.
     */
    public static final Pattern NODE_NAME_REGEX = Pattern.compile("[*|\\?:\"<>\\\\\\\\/]");

    public static final int FROM_INCOMING_SHARES = 140;
    public static final int FROM_BACKUPS = 150;

    public static final int SNACKBAR_TYPE = 0;
    public static final int MESSAGE_SNACKBAR_TYPE = 1;
    public static final int MUTE_NOTIFICATIONS_SNACKBAR_TYPE = 2;
    public static final int NOT_SPACE_SNACKBAR_TYPE = 3;
    public static final int PERMISSIONS_TYPE = 4;
    public static final int INVITE_CONTACT_TYPE = 5;
    public static final int DISMISS_ACTION_SNACKBAR = 6;
    public static final int OPEN_FILE_SNACKBAR_TYPE = 7;
    public static final int SENT_REQUESTS_TYPE = 8;

    public static final int NOT_CALL_PERMISSIONS_SNACKBAR_TYPE = 10;

    public static final int HEADER_VIEW_TYPE = 0;
    public static final int ITEM_VIEW_TYPE = 1;
    public static final int ITEM_PROGRESS = 2;
    public static final int ITEM_PLACEHOLDER_TYPE = 3;

    public static final int SCROLLING_UP_DIRECTION = -1;
    public static final int REQUIRE_PASSCODE_INVALID = -1;

    public static final String IS_NODE_INCOMING = "isNodeIncoming";
    public static final String CONTACT_HANDLE = "contactHandle";
    public static final String SHOW_SNACKBAR = "SHOW_SNACKBAR";
    public static final String CHAT_ID = "CHAT_ID";
    public static final String MESSAGE_ID = "messageId";
    public static final String CHAT_ID_OF_CURRENT_CALL = "chatHandleInProgress";
    public static final String CHAT_ID_OF_INCOMING_CALL = "chatHandleToAnswer";
    public static final String PEER_ID = "peerId";
    public static final String CLIENT_ID = "clientId";
    public static final String SELECTED_CONTACTS = "SELECTED_CONTACTS";
    public static final String NODE_HANDLES = "NODE_HANDLES";
    public static final String NAME = "name";
    public static final String HANDLE = "handle";
    public static final String HANDLE_LIST = "HANDLE_LIST";
    public static final String EMAIL = "email";
    public static final String UNKNOWN_USER_NAME_AVATAR = "unknown";
    public static final String VISIBLE_FRAGMENT = "VISIBLE_FRAGMENT";
    public static final String LAUNCH_INTENT = "LAUNCH_INTENT";
    public static final String SELECTED_CHATS = "SELECTED_CHATS";
    public static final String SELECTED_USERS = "SELECTED_USERS";
    public static final String ID_MESSAGES = "ID_MESSAGES";
    public static final String ID_CHAT_FROM = "ID_CHAT_FROM";
    public static final String USER_HANDLES = "USER_HANDLES";
    public static final String URL_FILE_LINK = "URL_FILE_LINK";
    public static final String OPEN_SCAN_QR = "OPEN_SCAN_QR";
    public static final String INVITE_CONTACT = "INVITE_CONTACT";
    public static final String TYPE_CALL_PERMISSION = "TYPE_CALL_PERMISSION";
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    public static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    public static final String COPIED_TEXT_LABEL = "Copied Text";
    public static final String IS_FLOATING_WINDOW = "IS_FLOATING_WINDOW";
    public static final String SCHEDULED_MEETING_ID = "SCHEDULED_MEETING_ID";
    public static final String SCHEDULED_MEETING_CREATED = "SCHEDULED_MEETING_CREATED";
    public static final int INVALID_POSITION = -1;
    public static final String INVALID_OPTION = "-1";
    public static final int INVALID_TYPE_PERMISSIONS = -1;
    public static final int INVALID_VOLUME = -1;
    public static final int INVALID_DIMENSION = -1;
    public static final int INVALID_CALL_STATUS = -1;
    public static final int INVALID_CALL = -1;

    public static final String SHOW_MESSAGE_UPLOAD_STARTED = "SHOW_MESSAGE_UPLOAD_STARTED";
    public static final String NUMBER_UPLOADS = "NUMBER_UPLOADS";
    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

    public static final int MAX_WIDTH_CONTACT_NAME_LAND = 450;
    public static final int MAX_WIDTH_CONTACT_NAME_PORT = 200;
    public static final int EMOJI_SIZE = 20;
    public static final int MAX_ALLOWED_CHARACTERS_AND_EMOJIS = 28;
    public static final int MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND = 350;
    public static final int MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT = 200;
    public static final int MAX_WIDTH_ADD_CONTACTS = 60;
    public static final int AVATAR_SIZE_CALLS = 50;
    public static final int AVATAR_SIZE_GRID = 75;
    public static final int AVATAR_SIZE = 150;
    public static final float MEETING_BOTTOM_MARGIN = 40f;
    public static final float MEETING_BOTTOM_MARGIN_WITH_KEYBOARD = 10f;
    public static final float MIN_MEETING_HEIGHT_CHANGE = 200;

    //Thumbnail dimens
    public static final float THUMB_CORNER_RADIUS_DP = 4;
    public static final int THUMB_SIZE_DP = 40;
    public static final int THUMB_MARGIN_DP = 16;
    public static final int ICON_SIZE_DP = 48;
    public static final int ICON_MARGIN_DP = 12;

    public static final float ALPHA_VIEW_DISABLED = 0.3f;
    public static final float ALPHA_VIEW_ENABLED = 1.0f;

    public static final int AUDIO_MANAGER_PLAY_VOICE_CLIP = 0;
    public static final int AUDIO_MANAGER_CALL_RINGING = 1;
    public static final int AUDIO_MANAGER_CALL_IN_PROGRESS = 2;
    public static final int AUDIO_MANAGER_CALL_OUTGOING = 3;
    public static final int AUDIO_MANAGER_CREATING_JOINING_MEETING = 4;

    public static final int CHECK_LINK_TYPE_UNKNOWN_LINK = 0;
    public static final int CHECK_LINK_TYPE_CHAT_LINK = 1;
    public static final int CHECK_LINK_TYPE_MEETING_LINK = 2;

    public static final String SEPARATOR = File.separator;

    /**
     * This Regex Pattern will check for the existence of:
     * 1. Domain with HTTPS protocol
     * 2. Followed by either: Mega.co.nz, Mega.nz, Mega.io, Megaad.nz
     * 3. No words are allowed after the domain name, for example; <a href="https://mega.co.nzxxx">...</a> is not allowed
     * 4. Backslashes (/) or Question Mark (?) are allowed to allow path and query parameters after the MEGA domain, for example; <a href="https://mega.nz/home">...</a>
     * 5. Any characters after Backslashes (/) or Question Mark (?) are allowed, except At Sign(@)
     */
    public static final String[] MEGA_REGEXS = {
            "^https://mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz)(\\/|\\?)[^@]*$",
            "^https://([a-z0-9]+\\.)+mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz)(\\/|\\?)[^@]*$"
    };

    public static final String[] FILE_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#!.+$",
            "^https://mega\\.nz/.*#!.+$",
            "^https://mega\\.co\\.nz/file/.+$",
            "^https://mega\\.nz/file/.+$",
    };

    public static final String[] CONFIRMATION_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#confirm.+$",
            "^https://mega\\.co\\.nz/.*confirm.+$",
            "^https://mega\\.nz/.*#confirm.+$",
            "^https://mega\\.nz/.*confirm.+$"
    };

    public static final String[] FOLDER_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#F!.+$",
            "^https://mega\\.nz/.*#F!.+$",
            "^https://mega\\.co\\.nz/folder/.+$",
            "^https://mega\\.nz/folder/.+$"
    };

    public static final String[] CHAT_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*chat/.+$",
            "^https://mega\\.nz/.*chat/.+$"
    };

    public static final String[] PASSWORD_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#P!.+$",
            "^https://mega\\.nz/.*#P!.+$"
    };

    public static final String[] ACCOUNT_INVITATION_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#newsignup.+$",
            "^https://mega\\.co\\.nz/.*newsignup.+$",
            "^https://mega\\.nz/.*#newsignup.+$",
            "^https://mega\\.nz/.*newsignup.+$"
    };

    public static final String[] EXPORT_MASTER_KEY_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#backup",
            "^https://mega\\.nz/.*#backup"
    };

    public static final String[] NEW_MESSAGE_CHAT_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#fm/chat",
            "^https://mega\\.co\\.nz/.*fm/chat",
            "^https://mega\\.nz/.*#fm/chat",
            "^https://mega\\.nz/.*fm/chat"
    };

    public static final String[] CANCEL_ACCOUNT_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#cancel.+$",
            "^https://mega\\.co\\.nz/.*cancel.+$",
            "^https://mega\\.nz/.*#cancel.+$",
            "^https://mega\\.nz/.*cancel.+$"
    };

    public static final String[] VERIFY_CHANGE_MAIL_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#verify.+$",
            "^https://mega\\.co\\.nz/.*verify.+$",
            "^https://mega\\.nz/.*#verify.+$",
            "^https://mega\\.nz/.*verify.+$"
    };

    public static final String[] RESET_PASSWORD_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#recover.+$",
            "^https://mega\\.co\\.nz/.*recover.+$",
            "^https://mega\\.nz/.*#recover.+$",
            "^https://mega\\.nz/.*recover.+$"
    };

    public static final String[] PENDING_CONTACTS_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#fm/ipc",
            "^https://mega\\.co\\.nz/.*fm/ipc",
            "^https://mega\\.nz/.*#fm/ipc",
            "^https://mega\\.nz/.*fm/ipc"
    };

    public static final String[] HANDLE_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#.+$",
            "^https://mega\\.nz/.*#.+$"
    };

    public static final String[] CONTACT_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/C!.+$",
            "^https://mega\\.nz/.*C!.+$"
    };

    public static final String[] MEGA_DROP_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*megadrop/.+$",
            "^https://mega\\.nz/.*megadrop/.+$"
    };

    public static final String[] MEGA_FILE_REQUEST_LINK_REGEXES = {
            "^https://mega\\.co\\.nz/.*filerequest/.+$",
            "^https://mega\\.nz/.*filerequest/.+$"
    };

    public static final String[] MEGA_BLOG_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#blog",
            "^https://mega\\.nz/.*#blog",
            "^https://mega\\.nz/.*blog",
            "^https://mega\\.co\\.nz/.*#blog.+$",
            "^https://mega\\.nz/.*#blog.+$",
            "^https://mega\\.nz/.*blog.+$"
    };

    public static final String[] REVERT_CHANGE_PASSWORD_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#pwr.+$",
            "^https://mega\\.co\\.nz/.*pwr.+$",
            "^https://mega\\.nz/.*#pwr.+$",
            "^https://mega\\.nz/.*pwr.+$"
    };

    public static final String[] EMAIL_VERIFY_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/#emailverify.+$",
            "^https://mega\\.co\\.nz/emailverify.+$",
            "^https://mega\\.nz/#emailverify.+$",
            "^https://mega\\.nz/emailverify.+$"
    };

    public static final String[] WEB_SESSION_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/#sitetransfer!.+$",
            "^https://mega\\.nz/#sitetransfer!.+$"
    };

    public static final String[] BUSINESS_INVITE_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/#businessinvite.+$",
            "^https://mega\\.co\\.nz/businessinvite.+$",
            "^https://mega\\.nz/#businessinvite.+$",
            "^https://mega\\.nz/businessinvite.+$"
    };

    public static final String[] ALBUM_LINK_REGEXS = {
            "^https://mega\\.nz/collection/.+$",
    };

    //Types of blocked accounts
    public static final String WEAK_PROTECTION_ACCOUNT_BLOCK = "700";

    public static final int INVALID_VALUE = -1;

    public static final long INVALID_SIZE = -1;

    public static final int LOCATION_INDEX_LEFT = 0;
    public static final int LOCATION_INDEX_TOP = 1;
    public static final int LOCATION_INDEX_WIDTH = 2;
    public static final int LOCATION_INDEX_HEIGHT = 3;

    public static final String OFFLINE_ROOT = "/";

    public static final long AUDIO_PLAYER_TRACK_NAME_FADE_DURATION_MS = 200;
    public static final long AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS = 3000;
    public static final long MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS = 300;
    public static final String KEY_IS_SHOWED_WARNING_MESSAGE = "is_showed_meeting_warning_message_";

    public static final String STRING_SEPARATOR = " · ";

    public static final int ORDER_CLOUD = 0;
    public static final int ORDER_OTHERS = 1;
    public static final int ORDER_CAMERA = 2;
    public static final int ORDER_OFFLINE = 3;
    public static final int ORDER_FAVOURITES = 4;
    public static final int ORDER_VIDEO_PLAYLIST = 5;
    public static final int ORDER_OUTGOING_SHARES = 6;

    public final static float MAX_WIDTH_APPBAR_LAND = 400;
    public final static float MAX_WIDTH_APPBAR_PORT = 200;

    public static final long ANIMATION_DURATION = 400;

    public static final String URL_INDICATOR = "URL=";

    /**
     * The param type returned by checkChatLink denoting the link is for a meeting room
     */
    public final static int LINK_IS_FOR_MEETING = 1;

    public static final float MEETING_NAME_MARGIN_TOP = 16f;

    public static final int NAME_CHANGE = 0;
    public static final int AVATAR_CHANGE = 1;

    public static final int FIRST_NAVIGATION_LEVEL = 0;

    public static final long LONG_SNACKBAR_DURATION = 2750;

    public static final String CANNOT_OPEN_FILE_SHOWN = "CANNOT_OPEN_FILE_SHOWN";

    public static final int MAX_TITLE_SIZE = 30;

    public static final int MAX_DESCRIPTION_SIZE = 3000;


    private static final String PACKAGE_NAME = "id=mega.privacy.android.app";
    public static final String MARKET_URI = "market://details?" + PACKAGE_NAME;
    public static final String PLAY_STORE_URI = "https://play.google.com/store/apps/details?" + PACKAGE_NAME;

    public static final String MEGA_VPN_URL = "https://vpn.mega.nz";
    public static final String MEGA_PASS_URL = "https://pwm.mega.nz";
    public static final String MEGA_TRANSFER_IT_URL = "https://transfer.it";
    public static final String MEGA_VPN_PACKAGE_NAME = "mega.vpn.android.app";
    public static final String MEGA_PASS_PACKAGE_NAME = "mega.pwm.android.app";
}
