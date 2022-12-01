package mega.privacy.android.app.utils;

import java.io.File;
import java.util.regex.Pattern;

public class Constants {

    public static final String PIN_4 = "4";
    public static final String PIN_6 = "6";
    public static final String PIN_ALPHANUMERIC = "alphanumeric";

    public static final int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels
    public static final int PHOTOS_UPLOAD_JOB_ID = 10096;
    public static final int BOOT_JOB_ID = 10097;

    public static final int REQUEST_CODE_GET_FILES = 1000;
    public static final int REQUEST_CODE_SELECT_FOLDER_TO_MOVE = 1001;
    public static final int REQUEST_CODE_SELECT_FOLDER_TO_COPY = 1002;
    public static final int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
    public static final int REQUEST_CODE_REFRESH = 1005;
    public static final int REQUEST_CODE_SORT_BY = 1006;
    public static final int REQUEST_CODE_SELECT_IMPORT_FOLDER = 1007;
    public static final int REQUEST_CODE_SELECT_FOLDER = 1008;
    public static final int REQUEST_CODE_SELECT_CONTACT = 1009;
    public static final int TAKE_PHOTO_CODE = 1010;
    public static final int WRITE_SD_CARD_REQUEST_CODE = 1011;
    public static final int REQUEST_CODE_SELECT_FILE = 1012;
    public static final int SET_PIN = 1013;
    public static final int REQUEST_CODE_TREE = 1014;
    public static final int TAKE_PICTURE_PROFILE_CODE = 1015;
    public static final int CHOOSE_PICTURE_PROFILE_CODE = 1016;
    public static final int REQUEST_INVITE_CONTACT_FROM_DEVICE = 1017;
    public static final int REQUEST_CREATE_CHAT = 1018;
    public static final int REQUEST_ADD_PARTICIPANTS = 1019;
    public static final int ENABLE_CHAT = 1020;
    public static final int REQUEST_SEND_CONTACTS = 1021;
    public static final int REQUEST_CODE_IMPORT_CHAT_NODE = 1022;
    public static final int REQUEST_CODE_IMPORT_CHAT_NODE_LIST = 1023;
    public static final int REQUEST_CODE_SELECT_CHAT = 1025;
    public static final int REQUEST_CODE_GET_CONTACTS = 1026;
    public static final int REQUEST_CODE_FILE_INFO = 1027;
    public static final int REQUEST_CODE_REFRESH_API_SERVER = 1028;
    public static final int REQUEST_CODE_DELETE_VERSIONS_HISTORY = 1029;
    public static final int REQUEST_CODE_SEND_LOCATION = 1030;
    public static final int REQUEST_CODE_COUNTRY_PICKER = 1031;
    public static final int REQUEST_CODE_VERIFY_CODE = 1032;
    public static final int REQUEST_CODE_SMS_VERIFICATION = 1033;
    public static final int REQUEST_CODE_SEND_LINK = 1035;
    public static final int REQUEST_CODE_SCAN_DOCUMENT = 1036;
    public static final int REQUEST_CODE_SEND_SEVERAL_LINKS = 1037;
    public static final int REQUEST_CODE_GET_FOLDER = 1038;
    public static final int REQUEST_CODE_GET_FOLDER_CONTENT = 1039;

    public static final String ACTION_REFRESH_AFTER_BLOCKED = "ACTION_REFRESH_AFTER_BLOCKED";
    public static final String ACTION_REFRESH = "ACTION_REFRESH";
    public static final String ACTION_REFRESH_API_SERVER = "ACTION_REFRESH_API_SERVER";
    public static final String ACTION_CREATE_ACCOUNT_EXISTS = "ACTION_CREATE_ACCOUNT_EXISTS";
    public static final String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
    public static final String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";

    public static final String ACTION_FORWARD_MESSAGES = "ACTION_FORWARD_MESSAGES";
    public static final String ACTION_OPEN_QR = "ACTION_OPEN_QR";
    public static final String ACTION_TAKE_PICTURE = "ACTION_TAKE_PICTURE";
    public static final String ACTION_TAKE_PROFILE_PICTURE = "ACTION_TAKE_PROFILE_PICTURE";
    public static final String ACTION_PREVIEW_GIPHY = "ACTION_PREVIEW_GIPHY";

    public static final String SHOW_REPEATED_UPLOAD = "SHOW_REPEATED_UPLOAD";

    public static final String EXTRA_SERIALIZE_STRING = "SERIALIZE_STRING";

    public static final String EXTRA_NODE_HANDLE = "NODE_HANDLE";
    public static final String EXTRA_RESULT_TRANSFER = "RESULT_TRANSFER";
    public static final String EXTRA_TRANSFER_TYPE = "TRANSFER_TYPE";
    public static final String EXTRA_USER_NICKNAME = "EXTRA_USER_NICKNAME";

    public static final String EXTRA_ACTION_RESULT = "EXTRA_ACTION_RESULT";

    public static final String FROM_HOME_PAGE = "FROM_HOME_PAGE";

    public static final String RESULT = "RESULT";
    public static final String ACCOUNT_BLOCKED_STRING = "ACCOUNT_BLOCKED_STRING";
    public static final String ACTION_SHOW_WARNING_ACCOUNT_BLOCKED = "ACTION_SHOW_WARNING_ACCOUNT_BLOCKED";

    public static final String EXTRA_STORAGE_STATE = "STORAGE_STATE";
    public static final String EXTRA_LINK = "EXTRA_LINK";
    public static final String EXTRA_SEVERAL_LINKS = "EXTRA_SEVERAL_LINKS";
    public static final String EXTRA_KEY = "EXTRA_KEY";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";

    public static final String EXTRA_MOVE_TO_CHAT_SECTION = "EXTRA_MOVE_TO_CHAT_SECTION";

    public static final String PREFERENCE_EMOJI = "emoji-recent-manager";
    public static final String PREFERENCE_REACTION = "reaction-recent-manager";
    public static final String PREFERENCE_VARIANT_EMOJI = "variant-emoji-manager";
    public static final String PREFERENCE_VARIANT_REACTION = "variant-reaction-manager";

    //MultipleRequestListener options
    public static final int MULTIPLE_MOVE = 0;
    public static final int MULTIPLE_SEND_RUBBISH = 1;
    //one file to many contacts
    public static final int MULTIPLE_CONTACTS_SEND_INBOX = 2;
    //many files to one contacts
    public static final int MULTIPLE_FILES_SEND_INBOX = 3;
    public static final int MULTIPLE_COPY = 4;
    public static final int MULTIPLE_REMOVE_SHARING_CONTACTS = 5;
    //one folder to many contacts
    public static final int MULTIPLE_CONTACTS_SHARE = 6;
    //one contact, many files
    public static final int MULTIPLE_FILE_SHARE = 7;
    public static final int MULTIPLE_LEAVE_SHARE = 8;

    public static final int MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER = 9;
    public static final int MULTIPLE_CHAT_IMPORT = 10;
    public static final int MULTIPLE_CHANGE_PERMISSION = 12;

    public static final int CANCEL_ACCOUNT_2FA = 4000;
    public static final int CHANGE_MAIL_2FA = 4001;
    public static final int DISABLE_2FA = 4002;
    public static final int CHANGE_PASSWORD_2FA = 4003;

    public static final int OVERQUOTA_ALERT = 5003;

    public static final int TOUR_FRAGMENT = 6000;
    public static final int LOGIN_FRAGMENT = 6001;
    public static final int CONFIRM_EMAIL_FRAGMENT = 6002;
    public static final int CREATE_ACCOUNT_FRAGMENT = 604;

    public static final int ACHIEVEMENTS_FRAGMENT = 8000;
    public static final int BONUSES_FRAGMENT = 8001;
    public static final int INVITE_FRIENDS_FRAGMENT = 8002;
    public static final int INFO_ACHIEVEMENTS_FRAGMENT = 8003;

    public static final int SCROLL_TO_POSITION = 9000;
    public static final int UPDATE_IMAGE_DRAG = 9001;
    public static final int UPDATE_GET_PRICING = 9002;
    public static final int UPDATE_ACCOUNT_DETAILS = 9003;
    public static final int UPDATE_CREDIT_CARD_SUBSCRIPTION = 9004;

    public static final int REQUEST_WRITE_STORAGE = 1;
    public static final int REQUEST_CAMERA = 2;
    public static final int REQUEST_READ_CONTACTS = 3;
    public static final int REQUEST_RECORD_AUDIO = 4;
    public static final int REQUEST_UPLOAD_CONTACT = 5;
    public static final int REQUEST_READ_STORAGE = 6;

    public static final int REQUEST_DOWNLOAD_FOLDER = 7;

    public static final int REQUEST_READ_WRITE_STORAGE = 9;

    public static final int REQUEST_CAMERA_UPLOAD = 10;
    public static final int REQUEST_CAMERA_ON_OFF = 11;
    public static final int REQUEST_CAMERA_ON_OFF_FIRST_TIME = 12;
    public static final int WRITE_LOG = 13;

    public static final int RECORD_VOICE_CLIP = 11;
    public static final int REQUEST_STORAGE_VOICE_CLIP = 12;
    public static final int REQUEST_CAMERA_TAKE_PICTURE = 13;
    public static final int REQUEST_WRITE_STORAGE_TAKE_PICTURE = 14;

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 15;
    public static final int REQUEST_BT_CONNECT = 16;
    public static final int REQUEST_CAMERA_SHOW_PREVIEW = 17;
    public static final int REQUEST_ACCESS_MEDIA_LOCATION = 17;

    public static final int TYPE_START_RECORD = 1;
    public static final int TYPE_END_RECORD = 2;
    public static final int TYPE_ERROR_RECORD = 3;

    public static final int IMPORT_ONLY_OPTION = 0;
    public static final int FORWARD_ONLY_OPTION = 1;
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
    public static final int SECONDS_IN_MONTH_31 = SECONDS_IN_DAY * 31;
    public static final int SECONDS_IN_YEAR = SECONDS_IN_DAY * 365;

    public static final long SECONDS_TO_WAIT_ALONE_ON_THE_CALL = 2 * SECONDS_IN_MINUTE;
    public static final long SECONDS_TO_WAIT_FOR_OTHERS_TO_JOIN_THE_CALL = 5 * SECONDS_IN_MINUTE;

    public static final int COLOR_STATUS_BAR_ACCENT = 1;
    public static final int COLOR_STATUS_BAR_ZERO_DELAY = 2;
    public static final int COLOR_STATUS_BAR_ZERO = 3;
    public static final int COLOR_STATUS_BAR_SEARCH_DELAY = 4;

    public static final String CONTACT_LINK_BASE_URL = "https://mega.nz/C!";
    public static final String DISPUTE_URL = "https://mega.nz/dispute";
    public static final String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
    public static final String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
    public static final String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
    public static final String ACTION_CANCEL_CAM_SYNC = "CANCEL_CAM_SYNC";
    public static final String ACTION_IMPORT_LINK_FETCH_NODES = "IMPORT_LINK_FETCH_NODES";
    public static final String ACTION_FILE_EXPLORER_UPLOAD = "FILE_EXPLORER_UPLOAD";
    public static final String ACTION_FILE_PROVIDER = "ACTION_FILE_PROVIDER";
    public static final String ACTION_EXPLORE_ZIP = "EXPLORE_ZIP";
    public static final String EXTRA_PATH_ZIP = "PATH_ZIP";
    public static final String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLDER";
    public static final String ACTION_REFRESH_PARENTHANDLE_BROWSER = "REFRESH_PARENTHANDLE_BROWSER";
    public static final String ACTION_OVERQUOTA_STORAGE = "OVERQUOTA_STORAGE";
    public static final String ACTION_TAKE_SELFIE = "TAKE_SELFIE";
    public static final String ACTION_SHOW_TRANSFERS = "SHOW_TRANSFERS";
    public static final String ACTION_EXPORT_MASTER_KEY = "EXPORT_MASTER_KEY";
    public static final String ACTION_OPEN_FOLDER = "OPEN_FOLDER";
    public static final String ACTION_CANCEL_ACCOUNT = "CANCEL_ACCOUNT";
    public static final String ACTION_RESET_PASS = "RESET_PASS";
    public static final String ACTION_RESET_PASS_FROM_LINK = "RESET_PASS_FROM_LINK";
    public static final String ACTION_PASS_CHANGED = "PASS_CHANGED";
    public static final String ACTION_PARK_ACCOUNT = "PARK_ACCOUNT";
    public static final String ACTION_RESET_PASS_FROM_PARK_ACCOUNT = "RESET_PASS_FROM_PARK_ACCOUNT";
    public static final String ACTION_CHANGE_MAIL = "CHANGE_MAIL";
    public static final String ACTION_CHANGE_AVATAR = "CHANGE_AVATAR";
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
    public static final String ACTION_LOG_OUT = "ACTION_LOG_OUT";
    public static final String ACTION_LOG_IN = "ACTION_LOG_IN";

    public static final String OPENED_FROM_CHAT = "OPENED_FROM_CHAT";
    public static final String OPENED_FROM_IMAGE_VIEWER = "OPENED_FROM_IMAGE_VIEWER";
    public static final String ACTION_OPEN_CHAT_LINK = "OPEN_CHAT_LINK";
    public static final String ACTION_JOIN_OPEN_CHAT_LINK = "JOIN_OPEN_CHAT_LINK";
    public static final String ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES";
    public static final String ACTION_UPDATE_ATTACHMENT = "UPDATE_ATTACHMENT";
    public static final String ACTION_OVERQUOTA_TRANSFER = "OVERQUOTA_TRANSFER";
    public static final String ACTION_SHOW_UPGRADE_ACCOUNT = "ACTION_SHOW_UPGRADE_ACCOUNT";
    public static final String ACTION_OPEN_CONTACTS_SECTION = "ACTION_OPEN_CONTACTS_SECTION";

    public static final String TYPE_EMOJI = "TYPE_EMOJI";
    public static final String TYPE_REACTION = "TYPE_REACTION";
    public static final String INVALID_REACTION = "INVALID_REACTION";

    public static final String ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD = "ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD";

    public static final String ACTION_RECOVERY_KEY_EXPORTED = "RECOVERY_KEY_EXPORTED";
    public static final String ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT = "REQUEST_DOWNLOAD_FOLDER_LOGOUT";

    public static final String ACTION_STORAGE_STATE_CHANGED = "ACTION_STORAGE_STATE_CHANGED";

    public static final String ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE = "ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE";

    public static final String ACTION_RESTART_SERVICE = "ACTION_RESTART_SERVICE";
    public static final String ACTION_CHECK_COMPRESSING_MESSAGE = "ACTION_CHECK_COMPRESSING_MESSAGE";

    public static final String ACTION_SHARE_MSG = "ACTION_SHARE_MSG";
    public static final String ACTION_SHARE_NODE = "ACTION_SHARE_NODE";
    public static final String ACTION_REMOVE_LINK = "ACTION_REMOVE_LINK";

    public static final String BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION = "INTENT_FILTER_UPDATE_POSITION";
    public static final String BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG = "INTENT_FILTER_UPDATE_IMAGE_DRAG";
    public static final String BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN = "INTENT_FILTER_UPDATE_FULL_SCREEN";

    public static final String BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS = "INTENT_UPDATE_ACCOUNT_DETAILS";
    public static final String BROADCAST_ACTION_INTENT_SETTINGS_UPDATED = "SETTINGS_UPDATED";
    public static final String BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED = "INTENT_SSL_VERIFICATION_FAILED";
    public static final String BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE = "INTENT_SIGNAL_PRESENCE";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_ORDER = "INTENT_UPDATE_ORDER";
    public static final String BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED = "INTENT_VOICE_CLIP_DOWNLOADED";
    public static final String BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED = "INTENT_BUSINESS_EXPIRED";
    public static final String BROADCAST_ACTION_INTENT_CHAT_ARCHIVED = "INTENT_CHAT_ARCHIVED";
    public static final String BROADCAST_ACTION_INTENT_CHAT_ARCHIVED_GROUP = "INTENT_CHAT_ARCHIVED_GROUP";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION = "BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_USER_DATA = "BROADCAST_ACTION_INTENT_UPDATE_USER_DATA";

    public static final String INTENT_EXTRA_KEY_PLACEHOLDER = "placeholder";
    public static final String INTENT_EXTRA_KEY_HANDLE = "HANDLE";
    public static final String INTENT_EXTRA_KEY_OFFLINE_HANDLE = "INTENT_EXTRA_KEY_OFFLINE_HANDLE";
    public static final String INTENT_EXTRA_KEY_FILE_NAME = "FILENAME";
    public static final String INTENT_EXTRA_KEY_SCREEN_POSITION = "screenPosition";
    public static final String INTENT_EXTRA_KEY_SCREEN_POSITION_FOR_SWIPE_DISMISS = "screenPositionForSwipeDismiss";
    public static final String INTENT_EXTRA_KEY_ADAPTER_TYPE = "adapterType";
    public static final String INTENT_EXTRA_KEY_VIEWER_FROM = "viewerFrom";
    public static final String INTENT_EXTRA_KEY_FROM_DOWNLOAD_SERVICE = "fromDownloadService";
    public static final String INTENT_EXTRA_KEY_INSIDE = "inside";
    public static final String INTENT_EXTRA_KEY_IS_URL = "isUrl";
    public static final String INTENT_EXTRA_KEY_POSITION = "position";
    public static final String INTENT_EXTRA_KEY_IS_FOLDER_LINK = "isFolderLink";
    public static final String INTENT_EXTRA_KEY_ORDER_GET_CHILDREN = "orderGetChildren";
    public static final String INTENT_EXTRA_KEY_PARENT_NODE_HANDLE = "parentNodeHandle";
    public static final String INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH = "handlesNodesSearch";
    public static final String INTENT_EXTRA_KEY_ARRAY_OFFLINE = "ARRAY_OFFLINE";
    public static final String INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY = "offlinePathDirectory";
    public static final String INTENT_EXTRA_KEY_PATH = "path";
    public static final String INTENT_EXTRA_KEY_PATH_NAVIGATION = "pathNavigation";
    public static final String INTENT_EXTRA_KEY_IS_LIST = "isList";
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
    public static final String INTENT_EXTRA_PENDING_MESSAGE_ID = "PENDING_MESSAGE_ID";
    public static final String INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER = "NEED_STOP_HTTP_SERVER";
    public static final String INTENT_EXTRA_KEY_FIRST_LEVEL = "firstLevel";
    public static final String INTENT_EXTRA_KEY_CHAT_ID = "chatId";
    public static final String INTENT_EXTRA_KEY_MSG_ID = "msgId";
    public static final String INTENT_EXTRA_KEY_ENABLED = "enabled";
    public static final String INTENT_EXTRA_KEY_CONTACT_TYPE = "contactType";
    public static final String INTENT_EXTRA_KEY_CHAT = "chat";
    public static final String INTENT_EXTRA_KEY_TOOL_BAR_TITLE = "aBtitle";
    public static final String INTENT_EXTRA_IS_FROM_MEETING = "extra_is_from_meeting";
    public static final String INTENT_EXTRA_MEETING_PARTICIPANTS = "participants_in_a_meeting";
    public static final String INTENT_EXTRA_KEY_URI = "INTENT_EXTRA_KEY_URI";
    public static final String INTENT_EXTRA_KEY_SHOW_NEARBY_FILES = "INTENT_EXTRA_KEY_SHOW_NEARBY_FILES";
    public static final String INTENT_EXTRA_KEY_IS_FILE_VERSION = "INTENT_EXTRA_KEY_IS_FILE_VERSION";
    public static final String INTENT_EXTRA_COLLISION_RESULTS = "INTENT_EXTRA_COLLISION_RESULTS";
    public static final String INTENT_EXTRA_SINGLE_COLLISION_RESULT = "INTENT_EXTRA_SINGLE_COLLISION_RESULT";

    public static final int FILE_BROWSER_ADAPTER = 2000;
    public static final int CONTACT_FILE_ADAPTER = 2001;
    public static final int RUBBISH_BIN_ADAPTER = 2002;
    public static final int SHARED_WITH_ME_ADAPTER = 2003;
    public static final int OFFLINE_ADAPTER = 2004;
    public static final int FOLDER_LINK_ADAPTER = 2005;
    public static final int SEARCH_ADAPTER = 2006;
    public static final int PHOTO_SYNC_ADAPTER = 2007;
    public static final int ZIP_ADAPTER = 2008;
    public static final int OUTGOING_SHARES_ADAPTER = 2009;
    public static final int INCOMING_SHARES_ADAPTER = 2010;
    public static final int INBOX_ADAPTER = 2011;
    public static final int INCOMING_REQUEST_ADAPTER = 2012;
    public static final int OUTGOING_REQUEST_ADAPTER = 2013;
    public static final int CAMERA_UPLOAD_ADAPTER = 2014;
    public static final int INCOMING_SHARES_PROVIDER_ADAPTER = 2016;
    public static final int CLOUD_DRIVE_PROVIDER_ADAPTER = 2017;
    public static final int SEARCH_BY_ADAPTER = 2018;
    public static final int FILE_LINK_ADAPTER = 2019;
    public static final int FROM_CHAT = 2020;
    public static final int CONTACT_SHARED_FOLDER_ADAPTER = 2021;
    public static final int FILE_INFO_SHARED_CONTACT_ADAPTER = 2022;
    public static final int GENERAL_OTHERS_ADAPTER = 2023;
    public static final int RECENTS_ADAPTER = 2024;
    public static final int LINKS_ADAPTER = 2025;
    public static final int PHOTOS_BROWSE_ADAPTER = 2026;
    public static final int PHOTOS_SEARCH_ADAPTER = 2027;
    public static final int AUDIO_BROWSE_ADAPTER = 2028;
    public static final int AUDIO_SEARCH_ADAPTER = 2029;
    public static final int DOCUMENTS_BROWSE_ADAPTER = 2030;
    public static final int DOCUMENTS_SEARCH_ADAPTER = 2031;
    public static final int VIDEO_BROWSE_ADAPTER = 2032;
    public static final int VIDEO_SEARCH_ADAPTER = 2033;
    public static final int RECENTS_BUCKET_ADAPTER = 2034;
    public static final int VERSIONS_ADAPTER = 2035;
    public static final int FROM_IMAGE_VIEWER = 2036;
    public static final int MEDIA_BROWSE_ADAPTER = 2037;
    public static final int ALBUM_CONTENT_ADAPTER = 2038;
    public static final int FAVOURITES_ADAPTER = 2039;

    public static final int VIEWER_FROM_PHOTOS = 1;
    public static final int VIEWER_FROM_INCOMING_SHARES = 2;
    public static final int VIEWER_FROM_OUTGOING_SHARES = 3;
    public static final int VIEWER_FROM_LINKS = 4;
    public static final int VIEWER_FROM_DOCUMENTS = 5;
    public static final int VIEWER_FROM_VIDEOS = 6;
    public static final int VIEWER_FROM_OFFLINE = 7;
    public static final int VIEWER_FROM_RECETS_BUCKET = 8;
    public static final int VIEWER_FROM_CUMU = 9;
    public static final int VIEWER_FROM_RECETS = 10;
    public static final int VIEWER_FROM_CONTACT_FILE_LIST = 11;
    public static final int VIEWER_FROM_FOLDER_LINK = 12;
    public static final int VIEWER_FROM_ZIP_BROWSER = 13;
    public static final int VIEWER_FROM_FILE_BROWSER = 14;
    public static final int VIEWER_FROM_INBOX = 15;
    public static final int VIEWER_FROM_RUBBISH_BIN = 16;
    public static final int VIEWER_FROM_SEARCH = 17;
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
    public static final int PAYMENT_METHODS_MIN_DIFFERENCE = 720;
    public static final int PRICING_MIN_DIFFERENCE = 720;
    public static final int EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE = 30;

    public static final String HISTORY_RETENTION_DISABLED = "HISTORY_RETENTION_DISABLED";
    public static final String HISTORY_RETENTION_1_DAY = "HISTORY_RETENTION_1_DAY";
    public static final String HISTORY_RETENTION_1_WEEK = "HISTORY_RETENTION_1_WEEK";
    public static final String HISTORY_RETENTION_1_MONTH = "HISTORY_RETENTION_1_MONTH";
    public static final String HISTORY_RETENTION_CUSTOM = "HISTORY_RETENTION_CUSTOM";

    public static final int CONTACT_TYPE_MEGA = 0;
    public static final int CONTACT_TYPE_DEVICE = 1;
    public static final int CONTACT_TYPE_BOTH = 2;

    public static final int TYPE_VOICE_CLIP = 3;

    public static final int SELECT_RINGTONE = 2000;
    public static final int SELECT_NOTIFICATION_SOUND = SELECT_RINGTONE + 1;

    public static final int DEVICE_ANDROID = 1;
    public static final int DEVICE_HUAWEI = 4;
    public static final int DEVICE_IOS = 2;

    public static final int NOTIFICATION_SUMMARY_CHAT = 0;
    public static final int NOTIFICATION_UPLOAD = 1;
    public static final int NOTIFICATION_DOWNLOAD = 2;
    public static final int NOTIFICATION_CAMERA_UPLOADS = 3;
    public static final int NOTIFICATION_DOWNLOAD_FINAL = 4;
    public static final int NOTIFICATION_UPLOAD_FINAL = 5;
    public static final int NOTIFICATION_CAMERA_UPLOADS_FINAL = 6;
    public static final int NOTIFICATION_PUSH_CLOUD_DRIVE = 7;
    public static final int NOTIFICATION_GENERAL_PUSH_CHAT = 8;
    public static final int NOTIFICATION_SUMMARY_INCOMING_CONTACT = 9;
    public static final int NOTIFICATION_STREAMING_OVERQUOTA = 10;
    public static final int NOTIFICATION_CALL_IN_PROGRESS = 11;
    public static final int NOTIFICATION_MISSED_CALL = 12;
    public static final int NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT = 13;
    public static final int NOTIFICATION_STORAGE_OVERQUOTA = 14;
    public static final int NOTIFICATION_CHAT_UPLOAD = 15;
    public static final int NOTIFICATION_UPLOAD_FOLDER = 16;
    public static final int NOTIFICATION_UPLOAD_FINAL_FOLDER = 17;

    public static final int SUCCESSFUL_VOICE_CLIP_TRANSFER = 1;
    public static final int ERROR_VOICE_CLIP_TRANSFER = 2;

    public static final String NOTIFICATION_CHANNEL_DOWNLOAD_ID = "DownloadServiceNotification";
    public static final String NOTIFICATION_CHANNEL_DOWNLOAD_NAME = "MEGA Download";
    public static final String NOTIFICATION_CHANNEL_UPLOAD_ID = "UploadServiceNotification";
    public static final String NOTIFICATION_CHANNEL_UPLOAD_NAME = "MEGA File Upload";
    public static final String NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID = "CameraUploadsServiceNotification";
    public static final String NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME = "MEGA Camera Uploads";
    public static final String NOTIFICATION_CHANNEL_CHAT_ID = "ChatNotification";
    public static final String NOTIFICATION_CHANNEL_CHAT_NAME = "MEGA Chat";
    public static final String NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID = "ChatSummaryNotification";
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
    public static final String NOTIFICATION_CHANNEL_FCM_FETCHING_MESSAGE = "MEGA Fetching Incoming Messages";
    public static final String NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID = "AudioPlayerNotification";
    public static final String CHAT_FOLDER = "My chat files";
    public static final String AUTHORITY_STRING_FILE_PROVIDER = "mega.privacy.android.app.providers.fileprovider";
    public static final String TYPE_TEXT_PLAIN = "text/plain";

    public static final int RICH_WARNING_TRUE = 1;
    public static final int RICH_WARNING_FALSE = 0;
    public static final int RICH_WARNING_CONFIRMATION = 2;

    public static final int TAKE_PICTURE_OPTION = 0;

    public static int TYPE_LEFT = -1;
    public static int TYPE_JOIN = 1;
    public static int TYPE_NETWORK_QUALITY = 2;
    public static int TYPE_AUDIO = 3;
    public static int TYPE_VIDEO = 4;

    public static final String MAIL_ANDROID = "androidfeedback@mega.nz";
    public static final String MAIL_SUPPORT = "support@mega.nz";

    //link for introduction end to end encryption
    public static final String URL_E2EE = "https://mega.nz/security";

    public static final int MIN_ITEMS_SCROLLBAR = 30;
    public static final int MIN_ITEMS_SCROLLBAR_GRID = 200;
    public static final int MIN_ITEMS_SCROLLBAR_CHAT = 20;
    public static final int MIN_ITEMS_SCROLLBAR_CONTACT = 20;

    public static final long BUFFER_COMP = 1073741824;      // 1 GB
    public static final int MAX_BUFFER_16MB = 16777216; // 16 MB
    public static final int MAX_BUFFER_32MB = 33554432; // 32 MB
    public static final int MAX_AUTOAWAY_TIMEOUT = 1457; //in minute, the max value supported by SDK

    public static final String HIGH_PRIORITY_TRANSFER = "HIGH_PRIORITY_TRANSFER";

    //Transfers app data parameters
    public static final String APP_DATA_VOICE_CLIP = "VOICE_CLIP";
    public static final String APP_DATA_CHAT = "CHAT_UPLOAD";
    public static final String APP_DATA_CU = "CU_UPLOAD";
    public static final String APP_DATA_SD_CARD = "SD_CARD_DOWNLOAD";
    public static final String APP_DATA_TXT_FILE = "TXT_FILE_UPLOAD";
    //Indicates the data after it, is the value of a transfer parameter
    public static final String APP_DATA_INDICATOR = ">";
    //Indicates the data after it, is a new transfer parameter
    public static final String APP_DATA_SEPARATOR = "-";
    //Indicates the data after it, is a new AppData due to a repeated transfer
    public static final String APP_DATA_REPEATED_TRANSFER_SEPARATOR = "!";
    public static final String APP_DATA_BACKGROUND_TRANSFER = "BACKGROUND_TRANSFER";

    public static final String AVATAR_PRIMARY_COLOR = "AVATAR_PRIMARY_COLOR";
    public static final String AVATAR_GROUP_CHAT_COLOR = "AVATAR_GROUP_CHAT_COLOR";
    public static final String AVATAR_PHONE_COLOR = "AVATAR_PHONE_COLOR";

    public static final int MAX_REACTIONS_PER_USER = 24;
    public static final int MAX_REACTIONS_PER_MESSAGE = 50;

    public static final int REACTION_ERROR_TYPE_MESSAGE = -1;
    public static final int REACTION_ERROR_DEFAULT_VALUE = 0;
    public static final int REACTION_ERROR_TYPE_USER = 1;

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
    public static final int FROM_INBOX = 150;
    public static final int FROM_OTHERS = 0;

    public static final int SNACKBAR_TYPE = 0;
    public static final int MESSAGE_SNACKBAR_TYPE = 1;
    public static final int MUTE_NOTIFICATIONS_SNACKBAR_TYPE = 2;
    public static final int NOT_SPACE_SNACKBAR_TYPE = 3;
    public static final int PERMISSIONS_TYPE = 4;
    public static final int INVITE_CONTACT_TYPE = 5;
    public static final int DISMISS_ACTION_SNACKBAR = 6;
    public static final int OPEN_FILE_SNACKBAR_TYPE = 7;
    public static final int SENT_REQUESTS_TYPE = 8;
    public static final int RESUME_TRANSFERS_TYPE = 9;

    public static final int INFO_ANIMATION = 3000;
    public static final int QUICK_INFO_ANIMATION = 500;

    public static final int HEADER_VIEW_TYPE = 0;
    public static final int ITEM_VIEW_TYPE = 1;
    public static final int ITEM_PROGRESS = 2;
    public static final int ITEM_PLACEHOLDER_TYPE = 3;

    public static final int FILE_LINK = 200;
    public static final int FOLDER_LINK = 201;
    public static final int CHAT_LINK = 202;
    public static final int CONTACT_LINK = 203;
    public static final int ERROR_LINK = -1;

    public static final int BACK_PRESS_NOT_HANDLED = 0;
    public static final int BACK_PRESS_HANDLED = 1;

    public static final int SCROLLING_UP_DIRECTION = -1;
    public static final int REQUIRE_PASSCODE_INVALID = -1;

    public static final String CONTACT_HANDLE = "contactHandle";
    public static final String SHOW_SNACKBAR = "SHOW_SNACKBAR";
    public static final String CHAT_ID = "CHAT_ID";
    public static final String MESSAGE_ID = "messageId";
    public static final String CALL_ID = "callId";
    public static final String CHAT_ID_OF_CURRENT_CALL = "chatHandleInProgress";
    public static final String CHAT_ID_OF_INCOMING_CALL = "chatHandleToAnswer";
    public static final String SECOND_CALL = "SECOND_CALL";
    public static final String PEER_ID = "peerId";
    public static final String CLIENT_ID = "clientId";
    public static final String CHAT_TITLE = "CHAT_TITLE";
    public static final String SELECTED_CONTACTS = "SELECTED_CONTACTS";
    public static final String NODE_HANDLES = "NODE_HANDLES";
    public static final String NAME = "name";
    public static final String HANDLE = "handle";
    public static final String HANDLE_LIST = "HANDLE_LIST";
    public static final String EMAIL = "email";
    public static final String UNKNOWN_USER_NAME_AVATAR = "unknown";
    public static final String VISIBLE_FRAGMENT = "VISIBLE_FRAGMENT";
    public static final String SELECTED_CHATS = "SELECTED_CHATS";
    public static final String SELECTED_USERS = "SELECTED_USERS";
    public static final String ID_MESSAGES = "ID_MESSAGES";
    public static final String ID_CHAT_FROM = "ID_CHAT_FROM";
    public static final String USER_HANDLES = "USER_HANDLES";
    public static final String URL_FILE_LINK = "URL_FILE_LINK";
    public static final String OPEN_SCAN_QR = "OPEN_SCAN_QR";
    public static final String TYPE_CAMERA = "TYPE_CAMERA";
    public static final String CHAT_LINK_EXTRA = "CHAT_LINK";
    public static final String WAITING_FOR_CALL = "WAITING_FOR_CALL";
    public static final String USER_WAITING_FOR_CALL = "USER_WAITING_FOR_CALL";
    public static final String TYPE_CALL_PERMISSION = "TYPE_CALL_PERMISSION";
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    public static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    public static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    public static final String COPIED_TEXT_LABEL = "Copied Text";
    public static final String IS_FROM_CONTACTS = "IS_FROM_CONTACTS";
    public static final String IS_FLOATING_WINDOW = "IS_FLOATING_WINDOW";
    public static final String SCHEDULED_MEETING_ID = "SCHEDULED_MEETING_ID";

    public static final int INVALID_POSITION = -1;
    public static final int INVALID_ID = -1;
    public static final String INVALID_OPTION = "-1";
    public static final int INVALID_TYPE_PERMISSIONS = -1;
    public static final int INVALID_VOLUME = -1;
    public static final int INVALID_DIMENSION = -1;
    public static final int INVALID_VIEW_TYPE = -1;
    public static final int INVALID_CALL_STATUS = -1;
    public static final int INVALID_CALL = -1;

    public static final String POSITION_SELECTED_MESSAGE = "POSITION_SELECTED_MESSAGE";

    public static final String SHOW_MESSAGE_UPLOAD_STARTED = "SHOW_MESSAGE_UPLOAD_STARTED";
    public static final String NUMBER_UPLOADS = "NUMBER_UPLOADS";
    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

    public static final String ACTION_CHAT_OPEN = "ACTION_CHAT_OPEN";

    public static final int MAX_WIDTH_CONTACT_NAME_LAND = 450;
    public static final int MAX_WIDTH_CONTACT_NAME_PORT = 200;
    public static final int MAX_WIDTH_CONTACT_NAME_GRID_LAND = 150;
    public static final int MAX_WIDTH_CONTACT_NAME_GRID_PORT = 120;
    public static final int EMOJI_SIZE = 20;
    public static final int EMOJI_SIZE_MEDIUM = 25;
    public static final int EMOJI_SIZE_HIGH = 30;
    public static final int EMOJI_SIZE_EXTRA_HIGH = 35;
    public static final int EMOJI_AVATAR_CALL_SMALL = 40;
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

    // Thumbnail dimens for Browse file page
    public static final int THUMBNAIL_SIZE_DP = 36;
    public static final int THUMBNAIL_MARGIN_DP = 18;

    public static final int AUDIO_MANAGER_PLAY_VOICE_CLIP = 0;
    public static final int AUDIO_MANAGER_CALL_RINGING = 1;
    public static final int AUDIO_MANAGER_CALL_IN_PROGRESS = 2;
    public static final int AUDIO_MANAGER_CALL_OUTGOING = 3;
    public static final int AUDIO_MANAGER_CREATING_JOINING_MEETING = 4;

    public static final int CHECK_LINK_TYPE_UNKNOWN_LINK = 0;
    public static final int CHECK_LINK_TYPE_CHAT_LINK = 1;
    public static final int CHECK_LINK_TYPE_MEETING_LINK = 2;

    public static final String SEPARATOR = File.separator;

    public static final String[] MEGA_REGEXS = {
            "^https://mega\\.co\\.nz.+$",
            "^https://mega\\.nz.+$",
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

    //Types of blocked accounts
    public static final String ACCOUNT_NOT_BLOCKED = "0";
    public static final String TOS_COPYRIGHT_ACCOUNT_BLOCK = "200";
    public static final String TOS_NON_COPYRIGHT_ACCOUNT_BLOCK = "300";
    public static final String DISABLED_BUSINESS_ACCOUNT_BLOCK = "400";
    public static final String REMOVED_BUSINESS_ACCOUNT_BLOCK = "401";
    public static final String SMS_VERIFICATION_ACCOUNT_BLOCK = "500";
    public static final String WEAK_PROTECTION_ACCOUNT_BLOCK = "700";

    public static final int INVALID_VALUE = -1;

    public static final long INVALID_SIZE = -1;
    public static final int COPY_FILE_BUFFER_SIZE = 32 * 1024; // 32 KB

    public static final int LOCATION_INDEX_LEFT = 0;
    public static final int LOCATION_INDEX_TOP = 1;
    public static final int LOCATION_INDEX_WIDTH = 2;
    public static final int LOCATION_INDEX_HEIGHT = 3;

    public static final String OFFLINE_ROOT = "/";

    public static final int SEARCH_BY_DATE_FILTER_POS_TYPE = 0;
    public static final int SEARCH_BY_DATE_FILTER_POS_THE_DAY = 1;
    public static final int SEARCH_BY_DATE_FILTER_POS_MONTH_OR_YEAR = 2;
    public static final int SEARCH_BY_DATE_FILTER_POS_START_DAY = 3;
    public static final int SEARCH_BY_DATE_FILTER_POS_END_DAY = 4;

    public static final long SEARCH_BY_DATE_FILTER_TYPE_ONE_DAY = 1;
    public static final long SEARCH_BY_DATE_FILTER_TYPE_LAST_MONTH_OR_YEAR = 2;
    public static final long SEARCH_BY_DATE_FILTER_TYPE_BETWEEN_TWO_DAYS = 3;

    public static final long SEARCH_BY_DATE_FILTER_LAST_MONTH = 1;
    public static final long SEARCH_BY_DATE_FILTER_LAST_YEAR = 2;

    public static final long AUDIO_PLAYER_BACKGROUND_PLAY_HINT_FADE_OUT_DURATION_MS = 3000;
    public static final long AUDIO_PLAYER_TRACK_NAME_FADE_DURATION_MS = 200;
    public static final long AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS = 3000;
    public static final long MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS = 400;

    public static final long GET_THUMBNAIL_THROTTLE_MS = 50;

    // 15360 GB = 15TB
    public static final int BUSINESS_ACCOUNT_STORAGE_SPACE_AMOUNT = 15360;

    public static final int MAX_BADGE_NUM = 9;

    public static final int START_CALL_AUDIO_ENABLE = 1;

    /** Event Keys */
    public static final String EVENT_NODES_CHANGE = "nodes_change";
    public static final String EVENT_ORDER_CHANGE = "order_change";
    public static final String EVENT_LIST_GRID_CHANGE = "list_grid_change";
    public static final String EVENT_NOTIFICATION_COUNT_CHANGE = "notification_count_change";
    public static final String EVENT_CHAT_STATUS_CHANGE = "chat_status_change";
    public static final String EVENT_LOGOUT_CLEARED = "logout_cleared";
    public static final String EVENT_HOMEPAGE_VISIBILITY = "homepage_visibility";
    public static final String EVENT_NOT_ALLOW_PLAY = "NOT_ALLOW_PLAY";
    public static final String EVENT_PSA = "EVENT_PSA";

    public static final String EVENT_DRAG_TO_EXIT_THUMBNAIL_VISIBILITY = "drag_to_exit_thumbnail_visibility";
    public static final String EVENT_DRAG_TO_EXIT_THUMBNAIL_LOCATION = "drag_to_exit_thumbnail_location";
    public static final String EVENT_DRAG_TO_EXIT_SCROLL = "drag_to_exit_scroll";

    public static final String EVENT_FAB_CHANGE = "fab_change";

    public static final String KEY_IS_SHOWED_WARNING_MESSAGE = "is_showed_meeting_warning_message_";

    /** In database, invalid value is defined as '-1' */
    public static final String INVALID_NON_NULL_VALUE = "-1";

    public static final String STRING_SEPARATOR = " · ";

    public static final int NOT_OVERQUOTA_STATE = 0;
    public static final int OVERQUOTA_STORAGE_STATE = 1;
    public static final int PRE_OVERQUOTA_STORAGE_STATE = 2;

    //Sort order management
    public static final String NEW_ORDER = "NEW_ORDER";
    public static final String IS_CLOUD_ORDER = "IS_CLOUD_ORDER";
    public static final int ORDER_CLOUD = 0;
    public static final int ORDER_OTHERS = 1;
    public static final int ORDER_CAMERA = 2;
    public static final int ORDER_OFFLINE = 3;
    public static final int ORDER_FAVOURITES = 4;

    public final static float MAX_WIDTH_APPBAR_LAND = 400;
    public final static float MAX_WIDTH_APPBAR_PORT = 200;

    public static final long ANIMATION_DURATION = 400;

    public static final String URL_INDICATOR = "URL=";

    /** The param type returned by checkChatLink denoting the link is for a meeting room */
    public final static int LINK_IS_FOR_MEETING = 1;

    // The name of the preference to retrieve.
    public final static String KEY_SHOW_EDUCATION = "show_education";

    // SharedPreference file name
    public final static String MEETINGS_PREFERENCE = "meeting_preference";

    public static final float MEETING_NAME_MARGIN_TOP = 16f;

    public static final int NAME_CHANGE = 0;
    public static final int AVATAR_CHANGE = 1;

    public static final int FIRST_NAVIGATION_LEVEL = 0;
    
    public static final long LONG_SNACKBAR_DURATION = 2750;

    public static final String CANNOT_OPEN_FILE_SHOWN = "CANNOT_OPEN_FILE_SHOWN";
}
