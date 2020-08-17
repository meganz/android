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

    public static final int REQUEST_CODE_GET = 1000;
    public static final int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
    public static final int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
    public static final int REQUEST_CODE_GET_LOCAL = 1003;
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
    public static final int ACTION_SEARCH_BY_DATE = 1024;
    public static final int REQUEST_CODE_SELECT_CHAT = 1025;
    public static final int REQUEST_CODE_GET_CONTACTS = 1026;
    public static final int REQUEST_CODE_FILE_INFO = 1027;
    public static final int REQUEST_CODE_REFRESH_STAGING = 1028;
    public static final int REQUEST_CODE_DELETE_VERSIONS_HISTORY = 1029;
    public static final int REQUEST_CODE_SEND_LOCATION = 1030;
    public static final int REQUEST_CODE_COUNTRY_PICKER = 1031;
    public static final int REQUEST_CODE_VERIFY_CODE = 1032;
    public static final int REQUEST_CODE_SMS_VERIFICATION = 1033;

    public static final String ACTION_REFRESH_AFTER_BLOCKED = "ACTION_REFRESH_AFTER_BLOCKED";
    public static final String ACTION_REFRESH = "ACTION_REFRESH";
    public static final String ACTION_REFRESH_STAGING = "ACTION_REFRESH_STAGING";
    public static final String ACTION_CREATE_ACCOUNT_EXISTS = "ACTION_CREATE_ACCOUNT_EXISTS";
    public static final String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
    public static final String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";

    public static final String ACTION_FORWARD_MESSAGES = "ACTION_FORWARD_MESSAGES";
    public static final String ACTION_OPEN_QR = "ACTION_OPEN_QR";
    public static final String ACTION_TAKE_PICTURE = "ACTION_TAKE_PICTURE";
    public static final String ACTION_TAKE_PROFILE_PICTURE = "ACTION_TAKE_PROFILE_PICTURE";

    public static final String SHOW_REPEATED_UPLOAD = "SHOW_REPEATED_UPLOAD";

    public static final String EXTRA_SERIALIZE_STRING = "SERIALIZE_STRING";

    public static final String EXTRA_NODE_HANDLE = "NODE_HANDLE";
    public static final String EXTRA_RESULT_TRANSFER = "RESULT_TRANSFER";
    public static final String EXTRA_TRANSFER_TYPE = "TRANSFER_TYPE";
    public static final String EXTRA_VOICE_CLIP = "VOICE_CLIP";
    public static final String EXTRA_USER_NICKNAME = "EXTRA_USER_NICKNAME";

    public static final String RESULT = "RESULT";
    public static final String ACCOUNT_BLOCKED_STRING = "ACCOUNT_BLOCKED_STRING";
    public static final String ACTION_SHOW_WARNING_ACCOUNT_BLOCKED = "ACTION_SHOW_WARNING_ACCOUNT_BLOCKED";

    public static final String EXTRA_STORAGE_STATE = "STORAGE_STATE";

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
    public static final int MULTIPLE_FORWARD_MESSAGES = 11;
    public static final int MULTIPLE_CHANGE_PERMISSION = 12;

    public static final int MULTIPLE_RESTORED_FROM_RUBBISH = MULTIPLE_FORWARD_MESSAGES + 1;

    public static final int CANCEL_ACCOUNT_2FA = 4000;
    public static final int CHANGE_MAIL_2FA = 4001;
    public static final int DISABLE_2FA = 4002;

    public static final int MY_ACCOUNT_FRAGMENT = 5000;
    public static final int UPGRADE_ACCOUNT_FRAGMENT = 5001;
    public static final int OVERQUOTA_ALERT = 5003;
    public static final int CC_FRAGMENT = 5004;
    public static final int FORTUMO_FRAGMENT = 5005;
    public static final int CENTILI_FRAGMENT = 5007;
    public static final int BACKUP_RECOVERY_KEY_FRAGMENT = 5008;

    public static final int PAYMENT_CC_MONTH = 111;
    public static final int PAYMENT_CC_YEAR = 112;

    public static final int TOUR_FRAGMENT = 6000;
    public static final int LOGIN_FRAGMENT = 6001;
    public static final int CONFIRM_EMAIL_FRAGMENT = 6002;
    public static final int CHOOSE_ACCOUNT_FRAGMENT = 6003;
    public static final int CREATE_ACCOUNT_FRAGMENT = 604;

    public static final int GET_LINK_FRAGMENT = 7000;
    public static final int COPYRIGHT_FRAGMENT = 7001;

    public static final int ACHIEVEMENTS_FRAGMENT = 8000;
    public static final int BONUSES_FRAGMENT = 8001;
    public static final int INVITE_FRIENDS_FRAGMENT = 8002;
    public static final int INFO_ACHIEVEMENTS_FRAGMENT = 8003;

    public static final int SCROLL_TO_POSITION = 9000;
    public static final int UPDATE_IMAGE_DRAG = 9001;
    public static final int UPDATE_GET_PRICING = 9002;
    public static final int UPDATE_ACCOUNT_DETAILS = 9003;
    public static final int UPDATE_CREDIT_CARD_SUBSCRIPTION = 9004;
    public static final int UPDATE_PAYMENT_METHODS = 9005;

    public static final int GO_OFFLINE = 9006;
    public static final int GO_ONLINE = 9007;
    public static final int START_RECONNECTION = 9008;

    public static final int REQUEST_WRITE_STORAGE = 1;
    public static final int REQUEST_CAMERA = 2;
    public static final int REQUEST_READ_CONTACTS = 3;
    public static final int RECORD_AUDIO = 4;
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
    public static final int REQUEST_WRITE_STORAGE_OFFLINE = 16;

    public static final int TYPE_START_RECORD = 1;
    public static final int TYPE_END_RECORD = 2;
    public static final int TYPE_ERROR_RECORD = 3;

    public static final int FREE = 0;
    public static final int PRO_I = 1;
    public static final int PRO_II = 2;
    public static final int PRO_III = 3;
    public static final int PRO_LITE = 4;
    public static final int BUSINESS = 100;


    public static final int COLOR_STATUS_BAR_ACCENT = 1;
    public static final int COLOR_STATUS_BAR_ZERO_DELAY = 2;
    public static final int COLOR_STATUS_BAR_ZERO = 3;
    public static final int COLOR_STATUS_BAR_SEARCH_DELAY = 4;
    public static final int COLOR_STATUS_BAR_SMS_VERIFICATION = 5;

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
    public static final String ACTION_CHILD_UPLOADED_OK = "ACTION_CHILD_UPLOADED_OK";
    public static final String ACTION_CHILD_UPLOADED_FAILED = "ACTION_CHILD_UPLOADED_FAILED";
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

    public static final String ACTION_OPEN_CHAT_LINK = "OPEN_CHAT_LINK";
    public static final String ACTION_JOIN_OPEN_CHAT_LINK = "JOIN_OPEN_CHAT_LINK";
    public static final String ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES";
    public static final String ACTION_CLEAR_CHAT = "CLEAR_CHAT";
    public static final String ACTION_UPDATE_ATTACHMENT = "UPDATE_ATTACHMENT";
    public static final String ACTION_OVERQUOTA_TRANSFER = "OVERQUOTA_TRANSFER";
    public static final String ACTION_SHOW_UPGRADE_ACCOUNT = "ACTION_SHOW_UPGRADE_ACCOUNT";
    public static final String ACTION_OPEN_CONTACTS_SECTION = "ACTION_OPEN_CONTACTS_SECTION";

    public static final String ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD = "ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD";

    public static final String ACTION_RECOVERY_KEY_EXPORTED = "RECOVERY_KEY_EXPORTED";
    public static final String ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT = "REQUEST_DOWNLOAD_FOLDER_LOGOUT";

    public static final String ACTION_STORAGE_STATE_CHANGED = "ACTION_STORAGE_STATE_CHANGED";

    public static final String ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE = "ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE";

    public static final String BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION = "INTENT_FILTER_UPDATE_POSITION";
    public static final String BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG = "INTENT_FILTER_UPDATE_IMAGE_DRAG";
    public static final String BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN = "INTENT_FILTER_UPDATE_FULL_SCREEN";

    public static final String BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS = "INTENT_UPDATE_ACCOUNT_DETAILS";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_2FA_SETTINGS = "INTENT_UPDATE_2FA_SETTINGS";
    public static final String BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE = "INTENT_CONNECTIVITY_CHANGE";
    public static final String BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE_DIALOG = "INTENT_CONNECTIVITY_CHANGE_DIALOG";
    public static final String BROADCAST_ACTION_INTENT_SETTINGS_UPDATED = "SETTINGS_UPDATED";
    public static final String BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED = "INTENT_SSL_VERIFICATION_FAILED";
    public static final String BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE = "INTENT_SIGNAL_PRESENCE";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_ORDER = "INTENT_UPDATE_ORDER";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_VIEW = "INTENT_UPDATE_VIEW";
    public static final String BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED = "INTENT_VOICE_CLIP_DOWNLOADED";
    public static final String BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED = "INTENT_BUSINESS_EXPIRED";
    public static final String BROADCAST_ACTION_INTENT_CHAT_ARCHIVED = "INTENT_CHAT_ARCHIVED";
    public static final String BROADCAST_ACTION_INTENT_CHAT_ARCHIVED_GROUP = "INTENT_CHAT_ARCHIVED_GROUP";
    public static final String BROADCAST_ACTION_INTENT_REFRESH_ADD_PHONE_NUMBER = "BROADCAST_ACTION_INTENT_REFRESH_ADD_PHONE_NUMBER";
    public static final String BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION = "BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION";

    public static final String INTENT_EXTRA_KEY_HANDLE = "HANDLE";
    public static final String INTENT_EXTRA_KEY_FILE_NAME = "FILENAME";
    public static final String INTENT_EXTRA_KEY_SCREEN_POSITION = "screenPosition";
    public static final String INTENT_EXTRA_KEY_ADAPTER_TYPE = "adapterType";
    public static final String INTENT_EXTRA_KEY_INSIDE = "inside";

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

    public static final int ACCOUNT_DETAILS_MIN_DIFFERENCE = 5;
    public static final int PAYMENT_METHODS_MIN_DIFFERENCE = 720;
    public static final int PRICING_MIN_DIFFERENCE = 720;
    public static final int EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE = 30;

    public static final int CONTACT_TYPE_MEGA = 0;
    public static final int CONTACT_TYPE_DEVICE = 1;
    public static final int CONTACT_TYPE_BOTH = 2;

    public static final int TYPE_VOICE_CLIP = 3;

    public static final int SELECT_RINGTONE = 2000;
    public static final int SELECT_NOTIFICATION_SOUND = SELECT_RINGTONE + 1;

    public static final int DEVICE_ANDROID = 1;
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
    public static final String NOTIFICATION_CHANNEL_UPLOAD_ID_FOLDER = "FolderUploadServiceNotification";
    public static final String NOTIFICATION_CHANNEL_UPLOAD_NAME = "MEGA File Upload";
    public static final String NOTIFICATION_CHANNEL_UPLOAD_NAME_FOLDER = "MEGA Folder Upload";
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
    public static final String CHAT_FOLDER = "My chat files";
    public static final String AUTHORITY_STRING_FILE_PROVIDER = "mega.privacy.android.app.providers.fileprovider";
    public static final String TYPE_TEXT_PLAIN = "text/plain";

    public static final int RICH_WARNING_TRUE = 1;
    public static final int RICH_WARNING_FALSE = 0;
    public static final int RICH_WARNING_CONFIRMATION = 2;

    public static final int TAKE_PICTURE_OPTION = 0;
    public static final int TAKE_PROFILE_PICTURE = 1;
    public static final int START_CALL_PERMISSIONS = 2;
    public static final int RETURN_CALL_PERMISSIONS = 3;

    public static final String MAIL_ANDROID = "androidfeedback@mega.nz";
    public static final String MAIL_SUPPORT = "support@mega.nz";

    //link for introduction end to end encryption
    public static final String URL_E2EE = "https://mega.nz/security";

    public static final int MIN_ITEMS_SCROLLBAR = 30;
    public static final int MIN_ITEMS_SCROLLBAR_GRID = 200;
    public static final int MIN_ITEMS_SCROLLBAR_CHAT = 20;

    public static final long BUFFER_COMP = 1073741824;      // 1 GB
    public static final int MAX_BUFFER_16MB = 16777216; // 16 MB
    public static final int MAX_BUFFER_32MB = 33554432; // 32 MB
    public static final int MAX_AUTOAWAY_TIMEOUT = 1457; //in minute, the max value supported by SDK

    public static final String HIGH_PRIORITY_TRANSFER = "HIGH_PRIORITY_TRANSFER";

    public static final String UPLOAD_APP_DATA_CHAT = "CHAT_UPLOAD";

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

    public static final int FROM_INCOMING_SHARES = 140;
    public static final int FROM_INBOX = 150;
    public static final int FROM_OTHERS = 0;

    public static final int SNACKBAR_TYPE = 0;
    public static final int MESSAGE_SNACKBAR_TYPE = 1;
    public static final int NOT_SPACE_SNACKBAR_TYPE = 3;

    public static final int INFO_ANIMATION = 3000;
    public static final int QUICK_INFO_ANIMATION = 500;

    public static final int HEADER_VIEW_TYPE = 0;
    public static final int ITEM_VIEW_TYPE = 1;
    public static final int ITEM_PROGRESS = 2;

    public static final int FILE_LINK = 200;
    public static final int FOLDER_LINK = 201;
    public static final int CHAT_LINK = 202;
    public static final int CONTACT_LINK = 203;
    public static final int ERROR_LINK = -1;
    public static final int INVALID_CALL_STATUS = -1;
    public static final int MAX_PARTICIPANTS_GRID = 6;

    public static final String CONTACT_HANDLE = "contactHandle";
    public static final String CHAT_ID = "chatHandle";
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
    public static final String EMAIL = "email";
    public static final String UNKNOWN_USER_NAME_AVATAR = "unknown";
    public static final String VISIBLE_FRAGMENT = "VISIBLE_FRAGMENT";
    public static final String SELECTED_CHATS = "SELECTED_CHATS";
    public static final String SELECTED_USERS = "SELECTED_USERS";
    public static final String ID_MESSAGES = "ID_MESSAGES";
    public static final String USER_HANDLES = "USER_HANDLES";
    public static final String URL_FILE_LINK = "URL_FILE_LINK";
    public static final String OPEN_SCAN_QR = "OPEN_SCAN_QR";
    public static final String TYPE_CAMERA = "TYPE_CAMERA";
    public static final String CHAT_LINK_EXTRA = "CHAT_LINK";
    public static final String WAITING_FOR_CALL = "WAITING_FOR_CALL";
    public static final String USER_WAITING_FOR_CALL = "USER_WAITING_FOR_CALL";
    public static final String TYPE_CALL_PERMISSION = "TYPE_CALL_PERMISSION";

    public static final int INVALID_POSITION = -1;
    public static final int INVALID_ID = -1;
    public static final int INVALID_TYPE_PERMISSIONS = -1;

    public static final String POSITION_SELECTED_MESSAGE = "POSITION_SELECTED_MESSAGE";

    public static final String SHOW_MESSAGE_UPLOAD_STARTED = "SHOW_MESSAGE_UPLOAD_STARTED";
    public static final String NUMBER_UPLOADS = "NUMBER_UPLOADS";

    public static final String REGISTER_BUSINESS_ACCOUNT = "registerb";

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
            "^https://mega\\.nz/.*#confirm.+$"
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
            "^https://mega\\.nz/.*#newsignup.+$"
    };

    public static final String[] EXPORT_MASTER_KEY_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#backup",
            "^https://mega\\.nz/.*#backup"
    };

    public static final String[] NEW_MESSAGE_CHAT_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#fm/chat",
            "^https://mega\\.nz/.*#fm/chat"
    };

    public static final String[] CANCEL_ACCOUNT_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#cancel.+$",
            "^https://mega\\.nz/.*#cancel.+$"
    };

    public static final String[] VERIFY_CHANGE_MAIL_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#verify.+$",
            "^https://mega\\.nz/.*#verify.+$"
    };

    public static final String[] RESET_PASSWORD_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#recover.+$",
            "^https://mega\\.nz/.*#recover.+$"
    };

    public static final String[] PENDING_CONTACTS_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/.*#fm/ipc",
            "^https://mega\\.nz/.*#fm/ipc"
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
            "^https://mega\\.nz/#emailverify.+$"
    };

    public static final String[] WEB_SESSION_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/#sitetransfer!.+$",
            "^https://mega\\.nz/#sitetransfer!.+$"
    };

    public static final String[] BUSINESS_INVITE_LINK_REGEXS = {
            "^https://mega\\.co\\.nz/#businessinvite.+$",
            "^https://mega\\.nz/#businessinvite.+$"
    };

    //Types of blocked accounts
    public static final String ACCOUNT_NOT_BLOCKED = "0";
    public static final String COPYRIGHT_ACCOUNT_BLOCK = "200";
    public static final String MULTIPLE_COPYRIGHT_ACCOUNT_BLOCK = "300";
    public static final String DISABLED_ACCOUNT_BLOCK = "400";
    public static final String REMOVED_ACCOUNT_BLOCK = "401";
    public static final String SMS_VERIFICATION_ACCOUNT_BLOCK = "500";
    public static final String WEAK_PROTECTION_ACCOUNT_BLOCK = "700";

    public static final int INVALID_VALUE = -1;

    public static final int LOCATION_INDEX_LEFT = 0;
    public static final int LOCATION_INDEX_TOP = 1;
}
