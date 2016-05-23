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

}
