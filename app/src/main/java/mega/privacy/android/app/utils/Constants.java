package mega.privacy.android.app.utils;

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
	public static int REQUEST_CODE_TREE = 1014;
	public static int TAKE_PICTURE_PROFILE_CODE = 1015;
	public static int CHOOSE_PICTURE_PROFILE_CODE = 1016;
	public static int REQUEST_INVITE_CONTACT_FROM_DEVICE = 1017;
	public static int REQUEST_CREATE_CHAT = 1018;
	public static int REQUEST_ADD_PARTICIPANTS = 1019;
	public static int ENABLE_CHAT = 1020;
	public static int REQUEST_SEND_CONTACTS = 1021;

	public static int REQUEST_CODE_IMPORT_CHAT_NODE = 1022;
	public static int REQUEST_CODE_IMPORT_CHAT_NODE_LIST = 1023;

	public static String ACTION_REFRESH = "ACTION_REFRESH";
	public static String ACTION_ENABLE_CHAT = "ACTION_ENABLE_CHAT";
	public static String ACTION_CREATE_ACCOUNT_EXISTS = "ACTION_CREATE_ACCOUNT_EXISTS";
	public static String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
	public static String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";

	public static String SHOW_REPEATED_UPLOAD = "SHOW_REPEATED_UPLOAD";

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

	public static final int REQUEST_WRITE_STORAGE = 1;
	public static final int REQUEST_CAMERA = 2;
	public static final int REQUEST_READ_CONTACTS = 3;

	public static final int PRO_LITE = 4;
	public static final int FREE = 0;
	public static final int PRO_I = 1;
	public static final int PRO_II = 2;
	public static final int PRO_III = 3;

	public static final int COLOR_STATUS_BAR_RED = 1;
	public static final int COLOR_STATUS_BAR_TRANSPARENT_BLACK = 2;

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
	public static String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLER";
	public static String ACTION_REFRESH_PARENTHANDLE_BROWSER = "REFRESH_PARENTHANDLE_BROWSER";
	public static String ACTION_OVERQUOTA_ALERT = "OVERQUOTA_ALERT";
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

	public static String ACTION_CHAT_NEW = "CHAT_NEW";
	public static String ACTION_CHAT_SHOW_MESSAGES = "CHAT_SHOW_MESSAGES";
	public static String ACTION_NEW_CHAT = "NEW_CHAT";
	public static String ACTION_CLEAR_CHAT = "CLEAR_CHAT";

	public static String ACTION_UPDATE_ATTACHMENT = "UPDATE_ATTACHMENT";

	public static String ACTION_OVERQUOTA_TRANSFER = "OVERQUOTA_TRANSFER";

	final public static int CHAT_ADAPTER_SHOW_ALL = 2;
	final public static int CHAT_ADAPTER_SHOW_TIME = 1;
	final public static int CHAT_ADAPTER_SHOW_NOTHING = 0;
	final public static int CHAT_ADAPTER_SHOW_NOTHING_NO_NAME = 3;

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
	final public static int NODE_ATTACHMENT_ADAPTER = 2015;

	final public static int ACCOUNT_DETAILS_MIN_DIFFERENCE = 5;
	final public static int PAYMENT_METHODS_MIN_DIFFERENCE = 720;
	final public static int PRICING_MIN_DIFFERENCE = 720;
	final public static int EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE = 30;

	final public static int CONTACT_TYPE_MEGA = 0;
	final public static int CONTACT_TYPE_DEVICE = 1;

	public static int SELECT_RINGTONE = 2000;
	public static int SELECT_NOTIFICATION_SOUND = SELECT_RINGTONE+1;

	public static int DEVICE_ANDROID = 1;
	public static int DEVICE_IOS = 2;

	public static int NOTIFICATION_UPLOAD = 1;
	public static int NOTIFICATION_DOWNLOAD = 2;
	public static int NOTIFICATION_CAMERA_UPLOADS = 3;
	public static int NOTIFICATION_DOWNLOAD_FINAL = 4;
	public static int NOTIFICATION_UPLOAD_FINAL = 5;
	public static int NOTIFICATION_CAMERA_UPLOADS_FINAL = 6;
	public static int NOTIFICATION_PUSH_CLOUD_DRIVE = 7;
	public static int NOTIFICATION_PUSH_CHAT = 8;
	public static int NOTIFICATION_PUSH_CONTACT = 9;

	public static String CHAT_FOLDER = "My chat files";

	public static int TAKE_PICTURE_OPTION = 0;
	public static int TAKE_PROFILE_PICTURE = 1;
}
