package mega.privacy.android.app.utils;

import java.util.regex.Pattern;

public class Constants {

	public static String PIN_4 = "4";
	public static String PIN_6 = "6";
	public static String PIN_ALPHANUMERIC = "alphanumeric";

	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels

	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_REFRESH = 1005;
	public static int REQUEST_CODE_SORT_BY = 1006;
	public static int REQUEST_CODE_SELECT_IMPORT_FOLDER = 1007;
	public static int REQUEST_CODE_SELECT_FOLDER = 1008;
	public static int REQUEST_CODE_SELECT_CONTACT = 1009;
	public static int TAKE_PHOTO_CODE = 1010;
	public static int WRITE_SD_CARD_REQUEST_CODE = 1011;
	public static int REQUEST_CODE_SELECT_FILE = 1012;
	public static int SET_PIN = 1013;
	public static int TAKE_PICTURE_PROFILE_CODE = 1015;
	public static int CHOOSE_PICTURE_PROFILE_CODE = 1016;
	public static int REQUEST_INVITE_CONTACT_FROM_DEVICE = 1017;
	public static int REQUEST_CREATE_CHAT = 1018;
	public static int REQUEST_ADD_PARTICIPANTS = 1019;
	public static int ENABLE_CHAT = 1020;
	public static int REQUEST_SEND_CONTACTS = 1021;
	public static int REQUEST_CODE_IMPORT_CHAT_NODE = 1022;
	public static int REQUEST_CODE_IMPORT_CHAT_NODE_LIST = 1023;
	public static int ACTION_SEARCH_BY_DATE = 1024;
	public static int REQUEST_CODE_SELECT_CHAT = 1025;
	public static int REQUEST_CODE_GET_CONTACTS = 1026;
	public static int REQUEST_CODE_FILE_INFO = 1027;
	public static int REQUEST_CODE_REFRESH_STAGING = 1028;
	public static int REQUEST_CODE_DELETE_VERSIONS_HISTORY = 1029;
    public static int REQUEST_CODE_TREE = REQUEST_CODE_DELETE_VERSIONS_HISTORY + 1;
	public static String ACTION_REFRESH = "ACTION_REFRESH";
	public static String ACTION_REFRESH_STAGING = "ACTION_REFRESH_STAGING";
	public static String ACTION_ENABLE_CHAT = "ACTION_ENABLE_CHAT";
	public static String ACTION_CREATE_ACCOUNT_EXISTS = "ACTION_CREATE_ACCOUNT_EXISTS";
	public static String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
	public static String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";

	public static String ACTION_FORWARD_MESSAGES = "ACTION_FORWARD_MESSAGES";

	public static String SHOW_REPEATED_UPLOAD = "SHOW_REPEATED_UPLOAD";

	public static String EXTRA_SERIALIZE_STRING = "SERIALIZE_STRING";

	//MultipleRequestListener options
	final public static int MULTIPLE_MOVE = 0;
	final public static int MULTIPLE_SEND_RUBBISH = MULTIPLE_MOVE+1;
	//one file to many contacts
	final public static int MULTIPLE_CONTACTS_SEND_INBOX = MULTIPLE_SEND_RUBBISH+1;
	//many files to one contacts
	final public static int MULTIPLE_FILES_SEND_INBOX = MULTIPLE_CONTACTS_SEND_INBOX+1;
	final public static int MULTIPLE_COPY = MULTIPLE_FILES_SEND_INBOX+1;
	final public static int MULTIPLE_REMOVE_SHARING_CONTACTS = MULTIPLE_COPY+1;
	//one folder to many contacts
	final public static int MULTIPLE_CONTACTS_SHARE = MULTIPLE_REMOVE_SHARING_CONTACTS+1;
	//one contact, many files
	final public static int MULTIPLE_FILE_SHARE = MULTIPLE_CONTACTS_SHARE+1;
	final public static int MULTIPLE_LEAVE_SHARE = MULTIPLE_FILE_SHARE+1;

	final public static int MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER = MULTIPLE_LEAVE_SHARE+1;
	final public static int MULTIPLE_CHAT_IMPORT = MULTIPLE_REMOVE_CONTACT_SHARED_FOLDER+1;
	final public static int MULTIPLE_FORWARD_MESSAGES = MULTIPLE_CHAT_IMPORT+1;

	final public static int MULTIPLE_RESTORED_FROM_RUBBISH = MULTIPLE_FORWARD_MESSAGES+1;

	final public static int CANCEL_ACCOUNT_2FA = 4000;
	final public static int CHANGE_MAIL_2FA = 4001;
	final public static int DISABLE_2FA = 4002;

	final public static int MY_ACCOUNT_FRAGMENT = 5000;
	final public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	final public static int OVERQUOTA_ALERT = 5003;
	final public static int CC_FRAGMENT = 5004;
	final public static int FORTUMO_FRAGMENT = 5005;
	final public static int MONTHLY_YEARLY_FRAGMENT = 5006;
	final public static int CENTILI_FRAGMENT = 5007;

	public static int PAYMENT_CC_MONTH = 111;
	public static int PAYMENT_CC_YEAR = 112;

	final public static int TOUR_FRAGMENT = 6000;
	final public static int LOGIN_FRAGMENT = 6001;
	final public static int CONFIRM_EMAIL_FRAGMENT = 6002;
	final public static int CHOOSE_ACCOUNT_FRAGMENT = 6003;
	final public static int CREATE_ACCOUNT_FRAGMENT = 604;

	final public static int GET_LINK_FRAGMENT = 7000;
	final public static int COPYRIGHT_FRAGMENT = 7001;

	final public static int ACHIEVEMENTS_FRAGMENT = 8000;
	final public static int BONUSES_FRAGMENT = 8001;
	final public static int INVITE_FRIENDS_FRAGMENT = 8002;
	final public static int INFO_ACHIEVEMENTS_FRAGMENT = 8003;

	final public static int SCROLL_TO_POSITION = 9000;
	final public static int UPDATE_IMAGE_DRAG = 9001;
	final public static int UPDATE_GET_PRICING = 9002;
	final public static int UPDATE_ACCOUNT_DETAILS = 9003;
	final public static int UPDATE_CREDIT_CARD_SUBSCRIPTION = 9004;
	final public static int UPDATE_PAYMENT_METHODS = 9005;

	final public static int GO_OFFLINE = 9006;
	final public static int GO_ONLINE = 9007;
	final public static int START_RECONNECTION = 9008;

	public static final int REQUEST_WRITE_STORAGE = 1;
	public static final int REQUEST_CAMERA = 2;
	public static final int REQUEST_READ_CONTACTS = 3;
	public static final int RECORD_AUDIO = 4;
	public static final int REQUEST_UPLOAD_CONTACT = 5;
	public static final int REQUEST_READ_STORAGE = 6;

	public static final int REQUEST_DOWNLOAD_FOLDER = 7;

	public static final int REQUEST_SAVE_MK_FROM_OFFLINE = 8;
	public static final int REQUEST_READ_WRITE_STORAGE = 9;

	public static final int WRITE_LOG = 10;

	public static final int PRO_LITE = 4;
	public static final int FREE = 0;
	public static final int PRO_I = 1;
	public static final int PRO_II = 2;
	public static final int PRO_III = 3;

	public static final int COLOR_STATUS_BAR_ACCENT = 1;
	public static final int COLOR_STATUS_BAR_ZERO_DELAY = 2;
	public static final int COLOR_STATUS_BAR_SEARCH = 3;
	public static final int COLOR_STATUS_BAR_ZERO = 4;
	public static final int COLOR_STATUS_BAR_SEARCH_DELAY = 5;

	public static String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
	public static String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
	public static String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
//	public static String ACTION_CANCEL_UPLOAD = "CANCEL_UPLOAD";
	public static String ACTION_CANCEL_CAM_SYNC = "CANCEL_CAM_SYNC";
	public static String ACTION_IMPORT_LINK_FETCH_NODES = "IMPORT_LINK_FETCH_NODES";
	public static String ACTION_FILE_EXPLORER_UPLOAD = "FILE_EXPLORER_UPLOAD";
	public static String ACTION_FILE_PROVIDER = "ACTION_FILE_PROVIDER";
	public static String ACTION_EXPLORE_ZIP = "EXPLORE_ZIP";
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLDER";
	public static String ACTION_REFRESH_PARENTHANDLE_BROWSER = "REFRESH_PARENTHANDLE_BROWSER";
	public static String ACTION_OVERQUOTA_STORAGE = "OVERQUOTA_STORAGE";
	public static String ACTION_TAKE_SELFIE = "TAKE_SELFIE";
	public static String ACTION_SHOW_TRANSFERS = "SHOW_TRANSFERS";
	public static String ACTION_EXPORT_MASTER_KEY = "EXPORT_MASTER_KEY";
	public static String ACTION_OPEN_FOLDER = "OPEN_FOLDER";
	public static String ACTION_CANCEL_ACCOUNT = "CANCEL_ACCOUNT";
	public static String ACTION_RESET_PASS = "RESET_PASS";
	public static String ACTION_RESET_PASS_FROM_LINK = "RESET_PASS_FROM_LINK";
	public static String ACTION_PASS_CHANGED = "PASS_CHANGED";
	public static String ACTION_PARK_ACCOUNT = "PARK_ACCOUNT";
	public static String ACTION_RESET_PASS_FROM_PARK_ACCOUNT = "RESET_PASS_FROM_PARK_ACCOUNT";
	public static String ACTION_CHANGE_MAIL = "CHANGE_MAIL";
	public static String ACTION_CHANGE_AVATAR = "CHANGE_AVATAR";
	public static String ACTION_IPC = "IPC";
	public static String ACTION_SHOW_MY_ACCOUNT = "ACTION_SHOW_MY_ACCOUNT";
	public static String ACTION_CHAT_NOTIFICATION_MESSAGE = "ACTION_CHAT_MESSAGE";
	public static String ACTION_CHAT_SUMMARY = "ACTION_CHAT_SUMMARY";
	public static String ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION = "ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION";
	public static String ACTION_OPEN_HANDLE_NODE = "ACTION_OPEN_HANDLE_NODE";
	public static String ACTION_OPEN_FILE_LINK_ROOTNODES_NULL = "ACTION_OPEN_FILE_LINK_ROOTNODES_NULL";
	public static String ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL = "ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL";
	public static String ACTION_SHOW_SETTINGS = "ACTION_SHOW_SETTINGS";
	public static String ACTION_SHOW_SETTINGS_STORAGE = "ACTION_SHOW_SETTINGS_STORAGE";
	public static String ACTION_PRE_OVERQUOTA_STORAGE = "PRE_OVERQUOTA_STORAGE";

	public static String ACTION_OPEN_CHAT_LINK = "OPEN_CHAT_LINK";
	public static String ACTION_JOIN_OPEN_CHAT_LINK = "JOIN_OPEN_CHAT_LINK";
	public static String ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES";
	public static String ACTION_CLEAR_CHAT = "CLEAR_CHAT";
	public static String ACTION_UPDATE_ATTACHMENT = "UPDATE_ATTACHMENT";
	public static String ACTION_OVERQUOTA_TRANSFER = "OVERQUOTA_TRANSFER";
	public static String ACTION_SHOW_UPGRADE_ACCOUNT = "ACTION_SHOW_UPGRADE_ACCOUNT";
	public static String ACTION_OPEN_CONTACTS_SECTION = "ACTION_OPEN_CONTACTS_SECTION";

	public static String ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD = "ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD";

	public static String ACTION_RECOVERY_KEY_EXPORTED = "RECOVERY_KEY_EXPORTED";
	public static String ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT = "REQUEST_DOWNLOAD_FOLDER_LOGOUT";

	public static String ACTION_STORAGE_STATE_CHANGED = "ACTION_STORAGE_STATE_CHANGED";

	public static String ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE = "ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE";

	public static String BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION = "INTENT_FILTER_UPDATE_POSITION";
	public static String BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG = "INTENT_FILTER_UPDATE_IMAGE_DRAG";
	public static String BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS = "INTENT_UPDATE_ACCOUNT_DETAILS";
	public static String BROADCAST_ACTION_INTENT_UPDATE_2FA_SETTINGS = "INTENT_UPDATE_2FA_SETTINGS";
	public static String BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE = "INTENT_CONNECTIVITY_CHANGE";
	public static String BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE_DIALOG = "INTENT_CONNECTIVITY_CHANGE_DIALOG";
	public static String BROADCAST_ACTION_INTENT_SETTINGS_UPDATED = "SETTINGS_UPDATED";
	public static String BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED = "INTENT_SSL_VERIFICATION_FAILED";
	public static String BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE = "INTENT_SIGNAL_PRESENCE";

	final public static int FILE_BROWSER_ADAPTER = 2000;
	final public static int CONTACT_FILE_ADAPTER = 2001;
	final public static int RUBBISH_BIN_ADAPTER = 2002;
	final public static int SHARED_WITH_ME_ADAPTER = 2003;
	final public static int OFFLINE_ADAPTER = 2004;
	final public static int FOLDER_LINK_ADAPTER = 2005;
	final public static int SEARCH_ADAPTER = 2006;
	final public static int PHOTO_SYNC_ADAPTER = 2007;
	final public static int ZIP_ADAPTER = 2008;
	final public static int OUTGOING_SHARES_ADAPTER = 2009;
	final public static int INCOMING_SHARES_ADAPTER = 2010;
	final public static int INBOX_ADAPTER = 2011;
	final public static int INCOMING_REQUEST_ADAPTER = 2012;
	final public static int OUTGOING_REQUEST_ADAPTER = 2013;
	final public static int CAMERA_UPLOAD_ADAPTER = 2014;
	final public static int INCOMING_SHARES_PROVIDER_ADAPTER = 2016;
	final public static int CLOUD_DRIVE_PROVIDER_ADAPTER = 2017;
	final public static int SEARCH_BY_ADAPTER = 2018;
	final public static int FILE_LINK_ADAPTER = 2019;
	final public static int FROM_CHAT = 2020;
	final public static int CONTACT_SHARED_FOLDER_ADAPTER = 2021;
    final public static int FILE_INFO_SHARED_CONTACT_ADAPTER = 2022;
	final public static int GENERAL_OTHERS_ADAPTER = 2023;

	final public static int ACCOUNT_DETAILS_MIN_DIFFERENCE = 5;
	final public static int PAYMENT_METHODS_MIN_DIFFERENCE = 720;
	final public static int PRICING_MIN_DIFFERENCE = 720;
	final public static int EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE = 30;

	final public static int CONTACT_TYPE_MEGA = 0;
	final public static int CONTACT_TYPE_DEVICE = 1;
	final public static int CONTACT_TYPE_BOTH = 2;

	public static int SELECT_RINGTONE = 2000;
	public static int SELECT_NOTIFICATION_SOUND = SELECT_RINGTONE+1;

	public static int DEVICE_ANDROID = 1;
	public static int DEVICE_IOS = 2;

	public static int NOTIFICATION_SUMMARY_CHAT = 0;
	public static int NOTIFICATION_UPLOAD = 1;
	public static int NOTIFICATION_DOWNLOAD = 2;
	public static int NOTIFICATION_CAMERA_UPLOADS = 3;
	public static int NOTIFICATION_DOWNLOAD_FINAL = 4;
	public static int NOTIFICATION_UPLOAD_FINAL = 5;
	public static int NOTIFICATION_CAMERA_UPLOADS_FINAL = 6;
	public static int NOTIFICATION_PUSH_CLOUD_DRIVE = 7;
	public static int NOTIFICATION_GENERAL_PUSH_CHAT = 8;
//	public static int NOTIFICATION_PUSH_CONTACT = 9;
	public static int NOTIFICATION_SUMMARY_INCOMING_CONTACT = 9;
	public static int NOTIFICATION_STREAMING_OVERQUOTA= 10;
	public static int NOTIFICATION_CALL_IN_PROGRESS = 11;
	public static int NOTIFICATION_MISSED_CALL = 12;
	public static int NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT = 13;
//	public static int NOTIFICATION_PRE_N_CHAT = 13;
	public static int NOTIFICATION_STORAGE_OVERQUOTA = 14;
	public static int NOTIFICATION_CHAT_UPLOAD = 15;
    public static int NOTIFICATION_UPLOAD_FOLDER = 16;
    public static int NOTIFICATION_UPLOAD_FINAL_FOLDER = 17;

	public static String NOTIFICATION_CHANNEL_DOWNLOAD_ID = "DownloadServiceNotification";
	public static String NOTIFICATION_CHANNEL_DOWNLOAD_NAME = "MEGA Download";
	public static String NOTIFICATION_CHANNEL_UPLOAD_ID = "UploadServiceNotification";
    public static String NOTIFICATION_CHANNEL_UPLOAD_ID_FOLDER = "FolderUploadServiceNotification";
	public static String NOTIFICATION_CHANNEL_UPLOAD_NAME = "MEGA File Upload";
    public static String NOTIFICATION_CHANNEL_UPLOAD_NAME_FOLDER = "MEGA Folder Upload";
	public static String NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID = "CameraUploadsServiceNotification";
	public static String NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME = "MEGA Camera Uploads";
	public static String NOTIFICATION_CHANNEL_CHAT_ID = "ChatNotification";
	public static String NOTIFICATION_CHANNEL_CHAT_NAME = "MEGA Chat";
	public static String NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID = "ChatSummaryNotification";
	public static String NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME = "MEGA Chat Summary";
	public static String NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID = "ChatSummaryNotificationNoVibrate";
	public static String NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME = "MEGA Chat Summary (no vibration)";
	public static String NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID = "InProgressMissedCallNotification";
	public static String NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME = "MEGA In Progress and Missed Calls";
	public static String NOTIFICATION_CHANNEL_INCOMING_CALLS_ID = "ChatIncomingCallNotification";
	public static String NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME = "MEGA Incoming Calls";
	public static String NOTIFICATION_CHANNEL_CONTACTS_ID = "ContactNotification";
	public static String NOTIFICATION_CHANNEL_CONTACTS_NAME = "MEGA Contact";
	public static String NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_ID = "ContactSummaryNotification";
	public static String NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_NAME = "MEGA Contact Summary";
	public static String NOTIFICATION_CHANNEL_CLOUDDRIVE_ID = "CloudDriveNotification";
	public static String NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME = "MEGA Cloud Drive";
	public static String NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID = "ChatUploadServiceNotification";
	public static String NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME = "MEGA Chat Upload";
    public static String NOTIFICATION_CHANNEL_FCM_FETCHING_MESSAGE = "MEGA Fetching Incoming Messages";


	public static String CHAT_FOLDER = "My chat files";

	public static int RICH_WARNING_TRUE = 1;
	public static int RICH_WARNING_FALSE= 0;
	public static int RICH_WARNING_CONFIRMATION = 2;

	public static int TAKE_PICTURE_OPTION = 0;
	public static int TAKE_PROFILE_PICTURE = 1;

	final public static String MAIL_ANDROID = "androidfeedback@mega.nz";
	final public static String MAIL_SUPPORT = "support@mega.nz";

	public static int MIN_ITEMS_SCROLLBAR = 30;
	public static int MIN_ITEMS_SCROLLBAR_GRID = 200;
	public static int MIN_ITEMS_SCROLLBAR_CHAT = 20;

	public static long BUFFER_COMP = 1073741824;      // 1 GB
	public static int MAX_BUFFER_16MB = 16777216; // 16 MB
	public static int MAX_BUFFER_32MB = 33554432; // 32 MB

	public static String HIGH_PRIORITY_TRANSFER = "HIGH_PRIORITY_TRANSFER";

	public static String UPLOAD_APP_DATA_CHAT = "CHAT_UPLOAD";

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

	public static int FROM_INCOMING_SHARES= 140;
	public static int FROM_INBOX= 150;

	public static final int SNACKBAR_TYPE = 0;
	public static final int MESSAGE_SNACKBAR_TYPE = 1;
	public static final int NOT_SPACE_SNACKBAR_TYPE = 3;
}
