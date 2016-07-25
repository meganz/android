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

	public static String ACTION_REFRESH = "ACTION_REFRESH";
	public static String ACTION_CREATE_ACCOUNT_EXISTS = "ACTION_CREATE_ACCOUNT_EXISTS";
	public static String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
	public static String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";

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

	final public static int MY_ACCOUNT_FRAGMENT = 5000;
	final public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	final public static int PAYMENT_FRAGMENT = 5002;
	final public static int OVERQUOTA_ALERT = 5003;
	final public static int CC_FRAGMENT = 5004;
	final public static int FORTUMO_FRAGMENT = 5005;
	final public static int MONTHLY_YEARLY_FRAGMENT = 5006;
	final public static int CENTILI_FRAGMENT = 5007;

	final public static int TOUR_FRAGMENT = 6000;
	final public static int LOGIN_FRAGMENT = 6001;
	final public static int CONFIRM_EMAIL_FRAGMENT = 6002;
	final public static int CHOOSE_ACCOUNT_FRAGMENT = 6003;
	final public static int CREATE_ACCOUNT_FRAGMENT = 604;

	public static final int REQUEST_WRITE_STORAGE = 1;
	public static final int REQUEST_CAMERA = 2;
	public static final int REQUEST_READ_CONTACTS = 3;

	public static String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
	public static String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
	public static String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
	public static String ACTION_CANCEL_UPLOAD = "CANCEL_UPLOAD";
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

	final public static int ACCOUNT_DETAILS_MIN_DIFFERENCE = 5;
	final public static int PAYMENT_METHODS_MIN_DIFFERENCE = 720;
	final public static int PRICING_MIN_DIFFERENCE = 720;
	final public static int EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE = 30;

}
