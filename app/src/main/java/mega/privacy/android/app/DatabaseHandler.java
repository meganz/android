package mega.privacy.android.app;

import static mega.privacy.android.app.constants.SettingsConstants.VIDEO_QUALITY_MEDIUM;
import static mega.privacy.android.app.constants.SettingsConstants.VIDEO_QUALITY_ORIGINAL;
import static mega.privacy.android.app.utils.Constants.INVALID_ID;
import static mega.privacy.android.app.utils.Constants.INVALID_OPTION;
import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.PIN_4;
import static mega.privacy.android.app.utils.Constants.REQUIRE_PASSCODE_INVALID;
import static mega.privacy.android.app.utils.LogUtil.areKarereLogsEnabled;
import static mega.privacy.android.app.utils.LogUtil.areSDKLogsEnabled;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logInfo;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.LogUtil.updateKarereLogs;
import static mega.privacy.android.app.utils.LogUtil.updateSDKLogs;
import static mega.privacy.android.app.utils.PasscodeUtil.REQUIRE_PASSCODE_IMMEDIATE;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.aes_decrypt;
import static mega.privacy.android.app.utils.Util.aes_encrypt;
import static nz.mega.sdk.MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS;
import static nz.mega.sdk.MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.domain.entity.SyncRecord;
import mega.privacy.android.app.domain.entity.SyncRecordType;
import mega.privacy.android.app.domain.entity.SyncStatus;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatItemPreferences;
import mega.privacy.android.app.main.megachat.ChatSettings;
import mega.privacy.android.app.main.megachat.NonContactInfo;
import mega.privacy.android.app.main.megachat.PendingMessageSingle;
import mega.privacy.android.app.objects.SDTransfer;
import mega.privacy.android.app.sync.Backup;
import mega.privacy.android.app.sync.BackupToolsKt;
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaTransfer;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 67;
    private static final String DATABASE_NAME = "megapreferences";
    private static final String TABLE_PREFERENCES = "preferences";
    private static final String TABLE_CREDENTIALS = "credentials";
    private static final String TABLE_ATTRIBUTES = "attributes";
    private static final String TABLE_OFFLINE = "offline";
    private static final String TABLE_CONTACTS = "contacts";
	private static final String TABLE_CHAT_ITEMS = "chat";
	private static final String TABLE_NON_CONTACTS = "noncontacts";
	private static final String TABLE_CHAT_SETTINGS = "chatsettings";
	private static final String TABLE_COMPLETED_TRANSFERS = "completedtransfers";
	private static final String TABLE_EPHEMERAL = "ephemeral";
	private static final String TABLE_PENDING_MSG = "pendingmsg";
	private static final String TABLE_MSG_NODES = "msgnodes";
	private static final String TABLE_NODE_ATTACHMENTS = "nodeattachments";
	private static final String TABLE_PENDING_MSG_SINGLE = "pendingmsgsingle";
	private static final String TABLE_SYNC_RECORDS = "syncrecords";
	private static final String TABLE_MEGA_CONTACTS = "megacontacts";
	private static final String TABLE_SD_TRANSFERS = "sdtransfers";
    public static final String TABLE_BACKUPS = "backups";

    private static final String KEY_ID = "id";
    private static final String KEY_EMAIL = "email";
	private static final String KEY_PASSWORD = "password";
    private static final String KEY_SESSION= "session";
	private static final String KEY_FIRST_NAME= "firstname";
	private static final String KEY_LAST_NAME= "lastname";
	private static final String KEY_MY_HANDLE= "myhandle";

    private static final String KEY_FIRST_LOGIN = "firstlogin";
    private static final String KEY_CAM_SYNC_ENABLED = "camsyncenabled";
    private static final String KEY_SEC_FOLDER_ENABLED = "secondarymediafolderenabled";
    private static final String KEY_SEC_FOLDER_HANDLE = "secondarymediafolderhandle";
    private static final String KEY_SEC_FOLDER_LOCAL_PATH = "secondarymediafolderlocalpath";
    private static final String KEY_CAM_SYNC_HANDLE = "camsynchandle";
    private static final String KEY_CAM_SYNC_WIFI = "wifi";
    private static final String KEY_CAM_SYNC_LOCAL_PATH = "camsynclocalpath";
    private static final String KEY_CAM_SYNC_FILE_UPLOAD = "fileUpload";
    private static final String KEY_CAM_SYNC_TIMESTAMP = "camSyncTimeStamp";
    private static final String KEY_CAM_VIDEO_SYNC_TIMESTAMP = "camVideoSyncTimeStamp";
    private static final String KEY_UPLOAD_VIDEO_QUALITY = "uploadVideoQuality";
    private static final String KEY_CONVERSION_ON_CHARGING = "conversionOnCharging";
    private static final String KEY_REMOVE_GPS = "removeGPS";
    private static final String KEY_CHARGING_ON_SIZE = "chargingOnSize";
    private static final String KEY_SHOULD_CLEAR_CAMSYNC_RECORDS = "shouldclearcamsyncrecords";
    private static final String KEY_KEEP_FILE_NAMES = "keepFileNames";
    private static final String KEY_SHOW_INVITE_BANNER = "showinvitebanner";
    private static final String KEY_ASK_FOR_DISPLAY_OVER = "askfordisplayover";
    private static final String KEY_PASSCODE_LOCK_ENABLED = "pinlockenabled";
    private static final String KEY_PASSCODE_LOCK_TYPE = "pinlocktype";
    private static final String KEY_PASSCODE_LOCK_CODE = "pinlockcode";
	private static final String KEY_PASSCODE_LOCK_REQUIRE_TIME = "passcodelockrequiretime";
	private static final String KEY_FINGERPRINT_LOCK= "fingerprintlock";
    private static final String KEY_STORAGE_ASK_ALWAYS = "storageaskalways";
    private static final String KEY_STORAGE_DOWNLOAD_LOCATION = "storagedownloadlocation";
    private static final String KEY_LAST_UPLOAD_FOLDER = "lastuploadfolder";
    private static final String KEY_LAST_CLOUD_FOLDER_HANDLE = "lastcloudfolder";
    private static final String KEY_ATTR_ONLINE = "online";
    private static final String KEY_ATTR_INTENTS = "intents";
    private static final String KEY_ATTR_ASK_SIZE_DOWNLOAD = "asksizedownload";
    private static final String KEY_ATTR_ASK_NOAPP_DOWNLOAD = "asknoappdownload";
    private static final String KEY_OFF_HANDLE = "handle";
    private static final String KEY_OFF_PATH = "path";
    private static final String KEY_OFF_NAME = "name";
    private static final String KEY_OFF_PARENT = "parentId";
    private static final String KEY_OFF_TYPE = "type";
    private static final String KEY_OFF_INCOMING = "incoming";
    private static final String KEY_OFF_HANDLE_INCOMING = "incomingHandle";
    private static final String KEY_SEC_SYNC_TIMESTAMP = "secondarySyncTimeStamp";
    private static final String KEY_SEC_VIDEO_SYNC_TIMESTAMP = "secondaryVideoSyncTimeStamp";
    private static final String KEY_STORAGE_ADVANCED_DEVICES = "storageadvanceddevices";
	private static final String KEY_ASK_SET_DOWNLOAD_LOCATION = "askSetDefaultDownloadLocation";
    private static final String KEY_PREFERRED_VIEW_LIST = "preferredviewlist";
    private static final String KEY_PREFERRED_VIEW_LIST_CAMERA = "preferredviewlistcamera";
    private static final String KEY_URI_EXTERNAL_SD_CARD = "uriexternalsdcard";
	private static final String KEY_URI_MEDIA_EXTERNAL_SD_CARD = "urimediaexternalsdcard";
    private static final String KEY_SD_CARD_URI = "sdcarduri";
    private static final String KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD = "camerafolderexternalsdcard";
	private static final String KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD = "mediafolderexternalsdcard";
    private static final String KEY_CONTACT_HANDLE = "handle";
    private static final String KEY_CONTACT_MAIL = "mail";
    private static final String KEY_CONTACT_NAME = "name";
    private static final String KEY_CONTACT_LAST_NAME = "lastname";
	private static final String KEY_CONTACT_NICKNAME = "nickname";
	private static final String KEY_PREFERRED_SORT_CLOUD = "preferredsortcloud";
	private static final String KEY_PREFERRED_SORT_CAMERA_UPLOAD = "preferredsortcameraupload";
	private static final String KEY_PREFERRED_SORT_OTHERS = "preferredsortothers";
	private static final String KEY_FILE_LOGGER_SDK = "filelogger";
	private static final String KEY_FILE_LOGGER_KARERE = "fileloggerkarere";
	private static final String KEY_USE_HTTPS_ONLY = "usehttpsonly";
	private static final String KEY_SHOW_COPYRIGHT = "showcopyright";
	private static final String KEY_SHOW_NOTIF_OFF = "shownotifoff";

	private static final String KEY_ACCOUNT_DETAILS_TIMESTAMP = "accountdetailstimestamp";
	private static final String KEY_PAYMENT_METHODS_TIMESTAMP = "paymentmethodsstimestamp";
	private static final String KEY_PRICING_TIMESTAMP = "pricingtimestamp";
	private static final String KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP = "extendedaccountdetailstimestamp";

	private static final String KEY_CHAT_HANDLE = "chathandle";
	private static final String KEY_CHAT_ITEM_NOTIFICATIONS = "chatitemnotifications";
	private static final String KEY_CHAT_ITEM_RINGTONE = "chatitemringtone";
	private static final String KEY_CHAT_ITEM_SOUND_NOTIFICATIONS = "chatitemnotificationsound";
	private static final String KEY_CHAT_ITEM_WRITTEN_TEXT = "chatitemwrittentext";
	private static final String KEY_CHAT_ITEM_EDITED_MSG_ID = "chatitemeditedmsgid";

	private static final String KEY_NONCONTACT_HANDLE = "noncontacthandle";
	private static final String KEY_NONCONTACT_FULLNAME = "noncontactfullname";
	private static final String KEY_NONCONTACT_FIRSTNAME = "noncontactfirstname";
	private static final String KEY_NONCONTACT_LASTNAME = "noncontactlastname";
	private static final String KEY_NONCONTACT_EMAIL = "noncontactemail";

	private static final String KEY_CHAT_NOTIFICATIONS_ENABLED = "chatnotifications";
	private static final String KEY_CHAT_SOUND_NOTIFICATIONS = "chatnotificationsound";
	private static final String KEY_CHAT_VIBRATION_ENABLED = "chatvibrationenabled";
	private static final String KEY_CHAT_VIDEO_QUALITY = "chatvideoQuality";

	private static final String KEY_INVALIDATE_SDK_CACHE = "invalidatesdkcache";

	private static final String KEY_TRANSFER_FILENAME = "transferfilename";
	private static final String KEY_TRANSFER_TYPE = "transfertype";
	private static final String KEY_TRANSFER_STATE = "transferstate";
	private static final String KEY_TRANSFER_SIZE = "transfersize";
	private static final String KEY_TRANSFER_HANDLE = "transferhandle";
	private static final String KEY_TRANSFER_PATH = "transferpath";
	private static final String KEY_TRANSFER_OFFLINE = "transferoffline";
	private static final String KEY_TRANSFER_TIMESTAMP = "transfertimestamp";
	private static final String KEY_TRANSFER_ERROR = "transfererror";
	private static final String KEY_TRANSFER_ORIGINAL_PATH = "transferoriginalpath";
	private static final String KEY_TRANSFER_PARENT_HANDLE = "transferparenthandle";
	public static final int MAX_TRANSFERS = 100;

	private static final String KEY_FIRST_LOGIN_CHAT = "firstloginchat";
    private static final String KEY_AUTO_PLAY = "autoplay";

	private static final String KEY_ID_CHAT = "idchat";
	private static final String KEY_MSG_TIMESTAMP = "timestamp";
	private static final String KEY_ID_TEMP_KARERE = "idtempkarere";
	private static final String KEY_STATE = "state";

	private static final String KEY_ID_PENDING_MSG = "idpendingmsg";
	private static final String KEY_ID_NODE = "idnode";

	private static final String KEY_FILE_PATH = "filepath";
	private static final String KEY_FILE_NAME = "filename";
	private static final String KEY_FILE_FINGERPRINT = "filefingerprint";
	private static final String KEY_NODE_HANDLE = "nodehandle";

	//columns for table sync records
    private static final String KEY_SYNC_FILEPATH_ORI = "sync_filepath_origin";
    private static final String KEY_SYNC_FILEPATH_NEW = "sync_filepath_new";
    private static final String KEY_SYNC_FP_ORI = "sync_fingerprint_origin";
    private static final String KEY_SYNC_FP_NEW = "sync_fingerprint_new";
    private static final String KEY_SYNC_TIMESTAMP = "sync_timestamp";
    private static final String KEY_SYNC_STATE = "sync_state";
    private static final String KEY_SYNC_FILENAME = "sync_filename";
    private static final String KEY_SYNC_HANDLE = "sync_handle";
    private static final String KEY_SYNC_COPYONLY = "sync_copyonly";
    private static final String KEY_SYNC_SECONDARY = "sync_secondary";
    private static final String KEY_SYNC_TYPE = "sync_type";
    private static final String KEY_SYNC_LONGITUDE = "sync_longitude";
    private static final String KEY_SYNC_LATITUDE = "sync_latitude";
    private static final String CREATE_SYNC_RECORDS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SYNC_RECORDS + "("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_SYNC_FILEPATH_ORI + " TEXT,"
            + KEY_SYNC_FILEPATH_NEW + " TEXT,"
            + KEY_SYNC_FP_ORI + " TEXT,"
            + KEY_SYNC_FP_NEW + " TEXT,"
            + KEY_SYNC_TIMESTAMP + " TEXT,"
            + KEY_SYNC_FILENAME + " TEXT,"
            + KEY_SYNC_LONGITUDE + " TEXT,"
            + KEY_SYNC_LATITUDE + " TEXT,"
            + KEY_SYNC_STATE + " INTEGER,"
            + KEY_SYNC_TYPE + " INTEGER,"
            + KEY_SYNC_HANDLE + " TEXT,"
            + KEY_SYNC_COPYONLY + " BOOLEAN,"
            + KEY_SYNC_SECONDARY + " BOOLEAN"+ ")";

	private static final String KEY_LAST_PUBLIC_HANDLE = "lastpublichandle";
	private static final String KEY_LAST_PUBLIC_HANDLE_TIMESTAMP = "lastpublichandletimestamp";
	private static final String KEY_LAST_PUBLIC_HANDLE_TYPE = "lastpublichandletype";
	private static final String KEY_STORAGE_STATE = "storagestate";
	private static final String KEY_MY_CHAT_FILES_FOLDER_HANDLE = "mychatfilesfolderhandle";
	private static final String KEY_TRANSFER_QUEUE_STATUS = "transferqueuestatus";

	private static final String KEY_PENDING_MSG_ID_CHAT = "idchat";
	private static final String KEY_PENDING_MSG_TIMESTAMP = "timestamp";
	private static final String KEY_PENDING_MSG_TEMP_KARERE = "idtempkarere";
	private static final String KEY_PENDING_MSG_FILE_PATH = "filePath";
	private static final String KEY_PENDING_MSG_NAME = "filename";
	private static final String KEY_PENDING_MSG_NODE_HANDLE = "nodehandle";
	private static final String KEY_PENDING_MSG_FINGERPRINT = "filefingerprint";
	private static final String KEY_PENDING_MSG_TRANSFER_TAG = "transfertag";
	private static final String KEY_PENDING_MSG_STATE = "state";

	private static final String KEY_MEGA_CONTACTS_ID = "userid";
	private static final String KEY_MEGA_CONTACTS_HANDLE = "handle";
	private static final String KEY_MEGA_CONTACTS_LOCAL_NAME = "localname";
	private static final String KEY_MEGA_CONTACTS_EMAIL = "email";
	private static final String KEY_MEGA_CONTACTS_PHONE_NUMBER = "phonenumber";
    private static final String CREATE_MEGA_CONTACTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MEGA_CONTACTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_MEGA_CONTACTS_ID + " TEXT,"
            + KEY_MEGA_CONTACTS_HANDLE + " TEXT,"
            + KEY_MEGA_CONTACTS_LOCAL_NAME + " TEXT,"
            + KEY_MEGA_CONTACTS_EMAIL + " TEXT,"
            + KEY_MEGA_CONTACTS_PHONE_NUMBER + " TEXT)";

	private static final String KEY_SD_TRANSFERS_TAG = "sdtransfertag";
	private static final String KEY_SD_TRANSFERS_NAME = "sdtransfername";
	private static final String KEY_SD_TRANSFERS_SIZE = "sdtransfersize";
	private static final String KEY_SD_TRANSFERS_HANDLE = "sdtransferhandle";
	private static final String KEY_SD_TRANSFERS_APP_DATA = "sdtransferappdata";
    private static final String KEY_SD_TRANSFERS_PATH = "sdtransferpath";
	private static final String CREATE_SD_TRANSFERS_TABLE = "CREATE TABLE IF NOT EXISTS "
			+ TABLE_SD_TRANSFERS + "("
			+ KEY_ID + " INTEGER PRIMARY KEY, " 	//0
			+ KEY_SD_TRANSFERS_TAG + " INTEGER, "	//1
			+ KEY_SD_TRANSFERS_NAME + " TEXT, "		//2
			+ KEY_SD_TRANSFERS_SIZE + " TEXT, "		//3
			+ KEY_SD_TRANSFERS_HANDLE + " TEXT, "	//4
			+ KEY_SD_TRANSFERS_PATH + " TEXT, "		//5
			+ KEY_SD_TRANSFERS_APP_DATA + " TEXT)";	//6

    public static final String KEY_BACKUP_ID = "backup_id";
    public static final String KEY_BACKUP_TYPE = "backup_type";
    public static final String KEY_BACKUP_TARGET_NODE = "target_node";
    public static final String KEY_BACKUP_LOCAL_FOLDER = "local_folder";
    public static final String KEY_BACKUP_NAME = "backup_name";
    public static final String KEY_BACKUP_STATE = "state";
    public static final String KEY_BACKUP_SUB_STATE = "sub_state";
    public static final String KEY_BACKUP_EXTRA_DATA = "extra_data";
    public static final String KEY_BACKUP_START_TIME = "start_timestamp";
    public static final String KEY_BACKUP_LAST_TIME = "last_sync_timestamp";
    public static final String KEY_BACKUP_TARGET_NODE_PATH = "target_folder_path";
    public static final String KEY_BACKUP_EX = "exclude_subolders";
    public static final String KEY_BACKUP_DEL = "delete_empty_subolders";
    public static final String KEY_BACKUP_OUTDATED = "outdated";

    private static final String CREATE_BACKUP_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_BACKUPS + "("
            + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_BACKUP_ID + " TEXT, "
            + KEY_BACKUP_TYPE + " INTEGER,"
            + KEY_BACKUP_TARGET_NODE + " TEXT,"
            + KEY_BACKUP_LOCAL_FOLDER + " TEXT,"
            + KEY_BACKUP_NAME + " TEXT,"
            + KEY_BACKUP_STATE + " INTEGER,"
            + KEY_BACKUP_SUB_STATE + " INTEGER,"
            + KEY_BACKUP_EXTRA_DATA + " TEXT,"
            + KEY_BACKUP_START_TIME + " TEXT,"
            + KEY_BACKUP_LAST_TIME + " TEXT,"
            + KEY_BACKUP_TARGET_NODE_PATH + " TEXT,"
            + KEY_BACKUP_EX + " BOOLEAN,"
            + KEY_BACKUP_DEL + " BOOLEAN,"
            + KEY_BACKUP_OUTDATED + " BOOLEAN)";

    private static final int OLD_VIDEO_QUALITY_ORIGINAL = 0;

    private static DatabaseHandler instance;

    private SQLiteDatabase db;

    public static synchronized DatabaseHandler getDbHandler(Context context){

        logDebug("getDbHandler");

    	if (instance == null){
            logDebug("INSTANCE IS NULL");
    		instance = new DatabaseHandler(context);
    	}

    	return instance;
    }

	public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
    }

	@Override
	public void onCreate(SQLiteDatabase db) {

        logDebug("onCreate");
        String CREATE_OFFLINE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_OFFLINE + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_OFF_HANDLE + " TEXT," + KEY_OFF_PATH + " TEXT," + KEY_OFF_NAME + " TEXT," +
        		KEY_OFF_PARENT + " INTEGER," + KEY_OFF_TYPE + " INTEGER, " + KEY_OFF_INCOMING + " INTEGER, " + KEY_OFF_HANDLE_INCOMING + " INTEGER "+")";
        db.execSQL(CREATE_OFFLINE_TABLE);

		String CREATE_CREDENTIALS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CREDENTIALS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_EMAIL + " TEXT, "
                + KEY_SESSION + " TEXT, " + KEY_FIRST_NAME + " TEXT, " +  KEY_LAST_NAME + " TEXT, " + KEY_MY_HANDLE + " TEXT" + ")";
        db.execSQL(CREATE_CREDENTIALS_TABLE);

        String CREATE_PREFERENCES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PREFERENCES + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"                  															//0
                + KEY_FIRST_LOGIN + " BOOLEAN, "                    															//1
                + KEY_CAM_SYNC_ENABLED + " BOOLEAN, "               															//2
                + KEY_CAM_SYNC_HANDLE + " TEXT, "                   															//3
                + KEY_CAM_SYNC_LOCAL_PATH + " TEXT, "               															//4
                + KEY_CAM_SYNC_WIFI + " BOOLEAN, "                  															//5
                + KEY_CAM_SYNC_FILE_UPLOAD + " TEXT, "              															//6
                + KEY_PASSCODE_LOCK_ENABLED + " TEXT, "               														    //7
                + KEY_PASSCODE_LOCK_CODE + " TEXT, "                   														    //8
                + KEY_STORAGE_ASK_ALWAYS + " TEXT, "                															//9
                + KEY_STORAGE_DOWNLOAD_LOCATION + " TEXT, "         															//10
                + KEY_CAM_SYNC_TIMESTAMP + " TEXT, "                															//11
                + KEY_LAST_UPLOAD_FOLDER + " TEXT, "                															//12
                + KEY_LAST_CLOUD_FOLDER_HANDLE + " TEXT, "          															//13
                + KEY_SEC_FOLDER_ENABLED + " TEXT, "                															//14
                + KEY_SEC_FOLDER_LOCAL_PATH + " TEXT, "             															//15
                + KEY_SEC_FOLDER_HANDLE + " TEXT, "                 															//16
                + KEY_SEC_SYNC_TIMESTAMP + " TEXT, "                															//17
                + KEY_KEEP_FILE_NAMES + " BOOLEAN, "                															//18
                + KEY_STORAGE_ADVANCED_DEVICES + " BOOLEAN, "       															//19
                + KEY_PREFERRED_VIEW_LIST + " BOOLEAN, "            															//20
                + KEY_PREFERRED_VIEW_LIST_CAMERA + " BOOLEAN, "     															//21
                + KEY_URI_EXTERNAL_SD_CARD + " TEXT, "              															//22
                + KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD + " BOOLEAN, " 															//23
                + KEY_PASSCODE_LOCK_TYPE + " TEXT, "                  														    //24
                + KEY_PREFERRED_SORT_CLOUD + " TEXT, "              															//25
                + KEY_PREFERRED_SORT_OTHERS + " TEXT,"              															//26
                + KEY_FIRST_LOGIN_CHAT + " BOOLEAN, "               															//27
                + KEY_AUTO_PLAY + " BOOLEAN,"                       															//28
                + KEY_UPLOAD_VIDEO_QUALITY + " TEXT DEFAULT '" + encrypt(String.valueOf(VIDEO_QUALITY_ORIGINAL))+ "',"			//29
                + KEY_CONVERSION_ON_CHARGING + " BOOLEAN,"          															//30
                + KEY_CHARGING_ON_SIZE + " TEXT,"                   															//31
                + KEY_SHOULD_CLEAR_CAMSYNC_RECORDS + " TEXT,"       															//32
                + KEY_CAM_VIDEO_SYNC_TIMESTAMP + " TEXT,"           															//33
                + KEY_SEC_VIDEO_SYNC_TIMESTAMP + " TEXT,"           															//34
                + KEY_REMOVE_GPS + " TEXT,"                         															//35
                + KEY_SHOW_INVITE_BANNER + " TEXT,"                 															//36
                + KEY_PREFERRED_SORT_CAMERA_UPLOAD + " TEXT,"       															//37
				+ KEY_SD_CARD_URI + " TEXT,"                        															//38
                + KEY_ASK_FOR_DISPLAY_OVER  + " TEXT,"																			//39
				+ KEY_ASK_SET_DOWNLOAD_LOCATION + " BOOLEAN,"																	//40
				+ KEY_URI_MEDIA_EXTERNAL_SD_CARD + " TEXT,"																		//41
				+ KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD + " BOOLEAN," 																//42
				+ KEY_PASSCODE_LOCK_REQUIRE_TIME + " TEXT DEFAULT '" + encrypt("" + (REQUIRE_PASSCODE_INVALID)) + "', "	//43
				+ KEY_FINGERPRINT_LOCK + " BOOLEAN DEFAULT '" + encrypt("false") + "'"									//44
				+ ")";

        db.execSQL(CREATE_PREFERENCES_TABLE);

		String CREATE_ATTRIBUTES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTRIBUTES + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, "                                                                                         //0
				+ KEY_ATTR_ONLINE + " TEXT, "                                                                                               //1
				+ KEY_ATTR_INTENTS + " TEXT, "                                                                                              //2
				+ KEY_ATTR_ASK_SIZE_DOWNLOAD + " BOOLEAN, "                                                                                 //3
				+ KEY_ATTR_ASK_NOAPP_DOWNLOAD + " BOOLEAN, "                                                                                //4
				+ KEY_ACCOUNT_DETAILS_TIMESTAMP + " TEXT, "                                                                                 //5
				+ KEY_PAYMENT_METHODS_TIMESTAMP + " TEXT, "                                                                                 //6
				+ KEY_PRICING_TIMESTAMP + " TEXT, "                                                                                         //7
				+ KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + " TEXT, "                                                                        //8
				+ KEY_INVALIDATE_SDK_CACHE + " TEXT, "                                                                                      //9
				+ KEY_USE_HTTPS_ONLY + " TEXT, "                                                                                            //10
				+ KEY_SHOW_COPYRIGHT + " TEXT, "                                                                                            //11
				+ KEY_SHOW_NOTIF_OFF + " TEXT, "                                                                                            //12
				+ KEY_LAST_PUBLIC_HANDLE + " TEXT, "                                                                                        //13
				+ KEY_LAST_PUBLIC_HANDLE_TIMESTAMP + " TEXT, "                                                                              //14
				+ KEY_STORAGE_STATE + " INTEGER DEFAULT '" + encrypt(String.valueOf(MegaApiJava.STORAGE_STATE_UNKNOWN)) + "',"              //15
				+ KEY_LAST_PUBLIC_HANDLE_TYPE + " INTEGER DEFAULT '" + encrypt(String.valueOf(MegaApiJava.AFFILIATE_TYPE_INVALID)) + "', "  //16
				+ KEY_MY_CHAT_FILES_FOLDER_HANDLE + " TEXT DEFAULT '" + encrypt(String.valueOf(MegaApiJava.INVALID_HANDLE)) + "', " 		//17
				+ KEY_TRANSFER_QUEUE_STATUS + " BOOLEAN DEFAULT '" + encrypt("false") + "'"                                          //18 - True if the queue is paused, false otherwise
				+ ")";
		db.execSQL(CREATE_ATTRIBUTES_TABLE);

        String CREATE_CONTACTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, "
				+ KEY_CONTACT_HANDLE + " TEXT, "
				+ KEY_CONTACT_MAIL + " TEXT, "
				+ KEY_CONTACT_NAME+ " TEXT, "
				+ KEY_CONTACT_LAST_NAME+ " TEXT, "
				+ KEY_CONTACT_NICKNAME+ " TEXT"+")";
        db.execSQL(CREATE_CONTACTS_TABLE);

		String CREATE_CHAT_ITEM_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_ITEMS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHAT_HANDLE + " TEXT, " + KEY_CHAT_ITEM_NOTIFICATIONS + " BOOLEAN, " +
				KEY_CHAT_ITEM_RINGTONE+ " TEXT, "+KEY_CHAT_ITEM_SOUND_NOTIFICATIONS+ " TEXT, "+KEY_CHAT_ITEM_WRITTEN_TEXT+ " TEXT, " +
				KEY_CHAT_ITEM_EDITED_MSG_ID + " TEXT"+")";
		db.execSQL(CREATE_CHAT_ITEM_TABLE);

		String CREATE_NONCONTACT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NON_CONTACTS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_NONCONTACT_HANDLE + " TEXT, " + KEY_NONCONTACT_FULLNAME + " TEXT, " +
				KEY_NONCONTACT_FIRSTNAME+ " TEXT, "+KEY_NONCONTACT_LASTNAME+ " TEXT, "+ KEY_NONCONTACT_EMAIL + " TEXT"+")";
		db.execSQL(CREATE_NONCONTACT_TABLE);

		String CREATE_CHAT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_SETTINGS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " 																//0
				+ KEY_CHAT_NOTIFICATIONS_ENABLED + " BOOLEAN, " 													//1
				+ KEY_CHAT_SOUND_NOTIFICATIONS + " TEXT, "															//2
				+ KEY_CHAT_VIBRATION_ENABLED + " BOOLEAN, " 														//3
				+ KEY_CHAT_VIDEO_QUALITY + " TEXT DEFAULT '"  + encrypt(String.valueOf(VIDEO_QUALITY_MEDIUM)) + "'"	//4
				+ ")";
		db.execSQL(CREATE_CHAT_TABLE);

		String CREATE_COMPLETED_TRANSFER_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_COMPLETED_TRANSFERS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, "						//0
				+ KEY_TRANSFER_FILENAME + " TEXT, "						//1
				+ KEY_TRANSFER_TYPE + " TEXT, "							//2
				+ KEY_TRANSFER_STATE + " TEXT, "						//3
				+ KEY_TRANSFER_SIZE + " TEXT, "							//4
				+ KEY_TRANSFER_HANDLE + " TEXT, "						//5
				+ KEY_TRANSFER_PATH + " TEXT, "							//6
				+ KEY_TRANSFER_OFFLINE + " BOOLEAN, "					//7
				+ KEY_TRANSFER_TIMESTAMP + " TEXT, "					//8
				+ KEY_TRANSFER_ERROR + " TEXT, "						//9
				+ KEY_TRANSFER_ORIGINAL_PATH + " TEXT, "				//10
				+ KEY_TRANSFER_PARENT_HANDLE + " TEXT"					//11
				+ ")";
		db.execSQL(CREATE_COMPLETED_TRANSFER_TABLE);

		String CREATE_EPHEMERAL = "CREATE TABLE IF NOT EXISTS " + TABLE_EPHEMERAL + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " +  KEY_EMAIL + " TEXT, "
				+ KEY_PASSWORD + " TEXT, " + KEY_SESSION + " TEXT, " +  KEY_FIRST_NAME + " TEXT, " + KEY_LAST_NAME + " TEXT" + ")";
		db.execSQL(CREATE_EPHEMERAL);

		String CREATE_PENDING_MSG_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PENDING_MSG + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_ID_CHAT + " TEXT, " + KEY_MSG_TIMESTAMP + " TEXT, " +KEY_ID_TEMP_KARERE + " TEXT, " + KEY_STATE + " INTEGER" +")";
		db.execSQL(CREATE_PENDING_MSG_TABLE);

		String CREATE_MSG_NODE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MSG_NODES + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_ID_PENDING_MSG+ " INTEGER, " + KEY_ID_NODE + " INTEGER" + ")";
		db.execSQL(CREATE_MSG_NODE_TABLE);

		String CREATE_NODE_ATTACHMENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NODE_ATTACHMENTS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_FILE_PATH + " TEXT, " + KEY_FILE_NAME + " TEXT, " + KEY_FILE_FINGERPRINT + " TEXT, " + KEY_NODE_HANDLE + " TEXT" + ")";
		db.execSQL(CREATE_NODE_ATTACHMENTS_TABLE);

		String CREATE_NEW_PENDING_MSG_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PENDING_MSG_SINGLE + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_PENDING_MSG_ID_CHAT + " TEXT, " + KEY_PENDING_MSG_TIMESTAMP + " TEXT, " +KEY_PENDING_MSG_TEMP_KARERE + " TEXT, " + KEY_PENDING_MSG_FILE_PATH + " TEXT, " + KEY_PENDING_MSG_NAME + " TEXT, " +KEY_PENDING_MSG_NODE_HANDLE + " TEXT, " +KEY_PENDING_MSG_FINGERPRINT + " TEXT, " + KEY_PENDING_MSG_TRANSFER_TAG + " INTEGER, " + KEY_PENDING_MSG_STATE + " INTEGER" +")";
		db.execSQL(CREATE_NEW_PENDING_MSG_TABLE);

        db.execSQL(CREATE_SYNC_RECORDS_TABLE);

        db.execSQL(CREATE_MEGA_CONTACTS_TABLE);

        db.execSQL(CREATE_SD_TRANSFERS_TABLE);

        db.execSQL(CREATE_BACKUP_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logInfo("Database upgraded from " + oldVersion + " to " + newVersion);

		//Used to identify when the Chat Settings table has been already recreated
        boolean chatSettingsAlreadyUpdated = false;
		//Used to identify when the Attributes table has been already recreated
        boolean attributesAlreadyUpdated = false;
        //Used to identify when the Preferences table has been already recreated
		boolean preferencesAlreadyUpdated = false;

		if (oldVersion <= 7){
			db.execSQL("ALTER TABLE " + TABLE_OFFLINE + " ADD COLUMN " + KEY_OFF_INCOMING + " INTEGER;");
			db.execSQL("ALTER TABLE " + TABLE_OFFLINE + " ADD COLUMN " + KEY_OFF_HANDLE_INCOMING + " INTEGER;");
			db.execSQL("UPDATE " + TABLE_OFFLINE + " SET " + KEY_OFF_INCOMING + " = '0';");
		}

		if (oldVersion <=8){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_LAST_UPLOAD_FOLDER + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_LAST_UPLOAD_FOLDER + " = '" + encrypt("") + "';");
		}

		if (oldVersion <=9){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_LAST_CLOUD_FOLDER_HANDLE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_LAST_CLOUD_FOLDER_HANDLE + " = '" + encrypt("") + "';");
		}

		if (oldVersion <=12){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SEC_FOLDER_ENABLED + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SEC_FOLDER_LOCAL_PATH + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SEC_FOLDER_HANDLE + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SEC_SYNC_TIMESTAMP + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_KEEP_FILE_NAMES + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_ENABLED + " = '" + encrypt("false") + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_LOCAL_PATH + " = '" + encrypt("-1") + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_HANDLE + " = '" + encrypt("-1") + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_SYNC_TIMESTAMP + " = '" + encrypt("0") + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_KEEP_FILE_NAMES + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <=13){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_STORAGE_ADVANCED_DEVICES + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_ADVANCED_DEVICES + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <=14){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_ATTR_INTENTS + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_INTENTS + " = '" + encrypt("0") + "';");
		}

		if (oldVersion <=15){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_VIEW_LIST + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_VIEW_LIST + " = '" + encrypt("true") + "';");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_VIEW_LIST_CAMERA + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_VIEW_LIST_CAMERA + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <=16){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_ATTR_ASK_SIZE_DOWNLOAD + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ASK_SIZE_DOWNLOAD + " = '" + encrypt("true") + "';");
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_ATTR_ASK_NOAPP_DOWNLOAD + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ASK_NOAPP_DOWNLOAD + " = '" + encrypt("true") + "';");

			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_URI_EXTERNAL_SD_CARD + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_URI_EXTERNAL_SD_CARD + " = '" + encrypt("") + "';");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <=17){
			String CREATE_CONTACTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + "("
	        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CONTACT_HANDLE + " TEXT, " + KEY_CONTACT_MAIL + " TEXT, " +
	        		KEY_CONTACT_NAME+ " TEXT, " + KEY_CONTACT_LAST_NAME + " TEXT"+")";
	        db.execSQL(CREATE_CONTACTS_TABLE);
		}

		if(oldVersion <= 18){
			//Changes to encrypt the Offline table
			ArrayList<MegaOffline> offlinesOld = this.getOfflineFilesOld(db);

            logDebug("Clear the table offline");
			this.clearOffline(db);

			for(int i=0; i<offlinesOld.size();i++){
				MegaOffline offline = offlinesOld.get(i);

				if(offline.getType()==null||offline.getType().equals("0")||offline.getType().equals("1")){
                    logDebug("Not encrypted: " + offline.getName());
					this.setOfflineFile(offline, db);	//using the method that encrypts
				}
				else{
                    logDebug("Encrypted: " + offline.getName());
					this.setOfflineFileOld(offline, db);	//using the OLD method that doesn't encrypt
				}
			}
		}

		if(oldVersion <= 19){

			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PASSCODE_LOCK_TYPE + " TEXT;");

			if(isPasscodeLockEnabled(db)){
                logDebug("PIN enabled!");
				db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PASSCODE_LOCK_TYPE + " = '" + encrypt(PIN_4) + "';");
			}
			else{
                logDebug("PIN NOT enabled!");
				db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PASSCODE_LOCK_TYPE + " = '" + encrypt("") + "';");
			}
		}

		if(oldVersion <= 20){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_SORT_CLOUD + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_SORT_OTHERS + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CLOUD + " = '" + encrypt(String.valueOf(MegaApiJava.ORDER_DEFAULT_ASC)) + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_OTHERS + " = '" + encrypt(String.valueOf(MegaApiJava.ORDER_DEFAULT_ASC)) + "';");
		}

		if(oldVersion <= 21){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_ACCOUNT_DETAILS_TIMESTAMP + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ACCOUNT_DETAILS_TIMESTAMP + " = '" + encrypt("") + "';");

			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_PAYMENT_METHODS_TIMESTAMP + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_PAYMENT_METHODS_TIMESTAMP + " = '" + encrypt("") + "';");

			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_PRICING_TIMESTAMP + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_PRICING_TIMESTAMP + " = '" + encrypt("") + "';");

			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + " = '" + encrypt("") + "';");
		}

		if(oldVersion <= 22) {
			String CREATE_CHAT_ITEM_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_ITEMS + "("
					+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHAT_HANDLE + " TEXT, " + KEY_CHAT_ITEM_NOTIFICATIONS + " BOOLEAN, " +
					KEY_CHAT_ITEM_RINGTONE + " TEXT, " + KEY_CHAT_ITEM_SOUND_NOTIFICATIONS + " TEXT" + ")";
			db.execSQL(CREATE_CHAT_ITEM_TABLE);

			String CREATE_NONCONTACT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NON_CONTACTS + "("
					+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_NONCONTACT_HANDLE + " TEXT, " + KEY_NONCONTACT_FULLNAME + " TEXT"+")";
			db.execSQL(CREATE_NONCONTACT_TABLE);

			String CREATE_CHAT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_SETTINGS + "("
					+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHAT_NOTIFICATIONS_ENABLED + " BOOLEAN, " +
					KEY_CHAT_SOUND_NOTIFICATIONS+ " TEXT, "+KEY_CHAT_VIBRATION_ENABLED+ " BOOLEAN"+")";
			db.execSQL(CREATE_CHAT_TABLE);
		}

		if (oldVersion <= 23){
			db.execSQL("ALTER TABLE " + TABLE_CREDENTIALS + " ADD COLUMN " + KEY_FIRST_NAME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_FIRST_NAME + " = '" + encrypt("") + "';");

			db.execSQL("ALTER TABLE " + TABLE_CREDENTIALS + " ADD COLUMN " + KEY_LAST_NAME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_LAST_NAME + " = '" + encrypt("") + "';");
		}

		if (oldVersion <= 25){
			db.execSQL("ALTER TABLE " + TABLE_NON_CONTACTS + " ADD COLUMN " + KEY_NONCONTACT_FIRSTNAME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_NON_CONTACTS + " SET " + KEY_NONCONTACT_FIRSTNAME + " = '" + encrypt("") + "';");

			db.execSQL("ALTER TABLE " + TABLE_NON_CONTACTS + " ADD COLUMN " + KEY_NONCONTACT_LASTNAME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_NON_CONTACTS + " SET " + KEY_NONCONTACT_LASTNAME + " = '" + encrypt("") + "';");

		}

		if (oldVersion <= 26){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_INVALIDATE_SDK_CACHE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_INVALIDATE_SDK_CACHE + " = '" + encrypt("true") + "';");
		}

		if (oldVersion <= 27){
			db.execSQL("ALTER TABLE " + TABLE_NON_CONTACTS + " ADD COLUMN " + KEY_NONCONTACT_EMAIL + " TEXT;");
			db.execSQL("UPDATE " + TABLE_NON_CONTACTS + " SET " + KEY_NONCONTACT_EMAIL + " = '" + encrypt("") + "';");
		}

		if (oldVersion <= 28){
			db.execSQL("ALTER TABLE " + TABLE_CREDENTIALS + " ADD COLUMN " + KEY_MY_HANDLE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_MY_HANDLE + " = '" + encrypt("") + "';");
		}

		if (oldVersion <= 29) {
			String CREATE_COMPLETED_TRANSFER_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_COMPLETED_TRANSFERS + "("
					+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_TRANSFER_FILENAME + " TEXT, " + KEY_TRANSFER_TYPE + " TEXT, " +
					KEY_TRANSFER_STATE+ " TEXT, "+ KEY_TRANSFER_SIZE+ " TEXT, " + KEY_TRANSFER_HANDLE + " TEXT"+")";
			db.execSQL(CREATE_COMPLETED_TRANSFER_TABLE);

		}

		if (oldVersion <= 30){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_FIRST_LOGIN_CHAT + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_FIRST_LOGIN_CHAT + " = '" + encrypt("true") + "';");
		}

		if (oldVersion <= 31){
			String CREATE_EPHEMERAL = "CREATE TABLE IF NOT EXISTS " + TABLE_EPHEMERAL + "("
					+ KEY_ID + " INTEGER PRIMARY KEY, " +  KEY_EMAIL + " TEXT, "
					+ KEY_PASSWORD + " TEXT, " + KEY_SESSION + " TEXT, " +  KEY_FIRST_NAME + " TEXT, " + KEY_LAST_NAME + " TEXT" + ")";
			db.execSQL(CREATE_EPHEMERAL);
		}

		if (oldVersion <= 32){
			String CREATE_PENDING_MSG_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PENDING_MSG + "("
					+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_ID_CHAT + " TEXT, " + KEY_MSG_TIMESTAMP + " TEXT, " +KEY_ID_TEMP_KARERE + " TEXT, " + KEY_STATE + " INTEGER" +")";
			db.execSQL(CREATE_PENDING_MSG_TABLE);

			String CREATE_MSG_NODE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MSG_NODES + "("
					+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_ID_PENDING_MSG+ " INTEGER, " + KEY_ID_NODE + " INTEGER" + ")";
			db.execSQL(CREATE_MSG_NODE_TABLE);

			String CREATE_NODE_ATTACHMENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NODE_ATTACHMENTS + "("
					+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_FILE_PATH + " TEXT, " + KEY_FILE_NAME + " TEXT, " + KEY_FILE_FINGERPRINT + " TEXT, " + KEY_NODE_HANDLE + " TEXT" + ")";
			db.execSQL(CREATE_NODE_ATTACHMENTS_TABLE);
		}

		if (oldVersion <= 34){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_USE_HTTPS_ONLY + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_USE_HTTPS_ONLY + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <= 35){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_SHOW_COPYRIGHT + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_SHOW_COPYRIGHT + " = '" + encrypt("true") + "';");
		}

		if (oldVersion <= 37){
			db.execSQL("ALTER TABLE " + TABLE_CHAT_ITEMS + " ADD COLUMN " + KEY_CHAT_ITEM_WRITTEN_TEXT + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CHAT_ITEMS + " SET " + KEY_CHAT_ITEM_WRITTEN_TEXT + " = '" + "" + "';");
		}

		if (oldVersion <= 38){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_SHOW_NOTIF_OFF + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_SHOW_NOTIF_OFF + " = '" + encrypt("true") + "';");
		}

		if (oldVersion <= 41){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_LAST_PUBLIC_HANDLE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_LAST_PUBLIC_HANDLE + " = '" + encrypt("-1") + "';");
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_LAST_PUBLIC_HANDLE_TIMESTAMP + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_LAST_PUBLIC_HANDLE_TIMESTAMP + " = '" + encrypt("-1") + "';");
		}

		if (oldVersion <= 42){
			String CREATE_NEW_PENDING_MSG_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PENDING_MSG_SINGLE + "("
					+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_PENDING_MSG_ID_CHAT + " TEXT, " + KEY_PENDING_MSG_TIMESTAMP + " TEXT, " +KEY_PENDING_MSG_TEMP_KARERE + " TEXT, " + KEY_PENDING_MSG_FILE_PATH + " TEXT, " + KEY_PENDING_MSG_NAME + " TEXT, " +KEY_PENDING_MSG_NODE_HANDLE + " TEXT, " +KEY_PENDING_MSG_FINGERPRINT + " TEXT, " + KEY_PENDING_MSG_TRANSFER_TAG + " INTEGER, " + KEY_PENDING_MSG_STATE + " INTEGER" +")";

			db.execSQL(CREATE_NEW_PENDING_MSG_TABLE);
		}

        if (oldVersion <= 43){
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_AUTO_PLAY + " BOOLEAN;");
            db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_AUTO_PLAY + " = '" + encrypt("false") + "';");
        }

		if(oldVersion <= 44) {
		    db.execSQL(CREATE_SYNC_RECORDS_TABLE);

            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_UPLOAD_VIDEO_QUALITY + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_CONVERSION_ON_CHARGING + " BOOLEAN;");
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_CHARGING_ON_SIZE + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SHOULD_CLEAR_CAMSYNC_RECORDS + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_CAM_VIDEO_SYNC_TIMESTAMP + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SEC_VIDEO_SYNC_TIMESTAMP + " TEXT;");
		}

        if(oldVersion <= 45) {
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_REMOVE_GPS + " TEXT;");
            db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_REMOVE_GPS + " = '" + encrypt("true") + "';");
        }

		if (oldVersion <= 46) {
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_STORAGE_STATE + " INTEGER;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_STORAGE_STATE + " = '" + encrypt(String.valueOf(MegaApiJava.STORAGE_STATE_UNKNOWN)) + "';");
		}

        if(oldVersion <= 47) {
            db.execSQL(CREATE_MEGA_CONTACTS_TABLE);

            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SHOW_INVITE_BANNER + " TEXT;");
            db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SHOW_INVITE_BANNER + " = '" + encrypt("true") + "';");
        }

		if(oldVersion <= 48) {
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_SORT_CAMERA_UPLOAD + " TEXT;");
            db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CAMERA_UPLOAD + " = '" + encrypt(String.valueOf(MegaApiJava.ORDER_MODIFICATION_DESC)) + "';");
        }

        if (oldVersion <= 49) {
            db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_SD_CARD_URI + " TEXT;");
        }

		if (oldVersion <= 50) {
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_ASK_FOR_DISPLAY_OVER + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_ASK_FOR_DISPLAY_OVER + " = '" + encrypt("true") + "';");
		}

		if (oldVersion <= 51) {
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_LAST_PUBLIC_HANDLE_TYPE + " INTEGER;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_LAST_PUBLIC_HANDLE_TYPE + " = '" + encrypt(String.valueOf(MegaApiJava.AFFILIATE_TYPE_INVALID)) + "';");
		}

		if (oldVersion <= 52) {
			recreateChatSettings(db, getChatSettingsFromDBv52(db));
			chatSettingsAlreadyUpdated = true;
		}

		if (oldVersion <= 53) {
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_ASK_SET_DOWNLOAD_LOCATION + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_ASK_SET_DOWNLOAD_LOCATION + " = '" + encrypt("true") + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_ASK_ALWAYS + " = '" + encrypt("true") + "';");
			db.execSQL("ALTER TABLE " + TABLE_COMPLETED_TRANSFERS + " ADD COLUMN " + KEY_TRANSFER_PATH + " TEXT;");
		}

		if (oldVersion <= 54) {
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_MY_CHAT_FILES_FOLDER_HANDLE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_MY_CHAT_FILES_FOLDER_HANDLE + " = '" + encrypt(String.valueOf(INVALID_HANDLE)) + "';");
		}

		if (oldVersion <= 55) {
			db.execSQL("ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN " + KEY_CONTACT_NICKNAME + " TEXT;");
		}

		if (oldVersion <= 56){
			db.execSQL("ALTER TABLE " + TABLE_CHAT_ITEMS + " ADD COLUMN " + KEY_CHAT_ITEM_EDITED_MSG_ID + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CHAT_ITEMS + " SET " + KEY_CHAT_ITEM_EDITED_MSG_ID + " = '" + "" + "';");
		}

		if (oldVersion <= 57) {
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_URI_MEDIA_EXTERNAL_SD_CARD + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_URI_MEDIA_EXTERNAL_SD_CARD + " = '" + encrypt("") + "';");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD + " = '" + encrypt("false") + "';");

			db.execSQL("ALTER TABLE " + TABLE_COMPLETED_TRANSFERS + " ADD COLUMN " + KEY_TRANSFER_OFFLINE + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_COMPLETED_TRANSFERS + " SET " + KEY_TRANSFER_OFFLINE + " = '" + encrypt("false") + "';");
			db.execSQL("ALTER TABLE " + TABLE_COMPLETED_TRANSFERS + " ADD COLUMN " + KEY_TRANSFER_TIMESTAMP + " TEXT;");
			db.execSQL("UPDATE " + TABLE_COMPLETED_TRANSFERS + " SET " + KEY_TRANSFER_TIMESTAMP + " = '" + encrypt(System.currentTimeMillis() + "") + "';");
			db.execSQL("ALTER TABLE " + TABLE_COMPLETED_TRANSFERS + " ADD COLUMN " + KEY_TRANSFER_ERROR + " TEXT;");
			db.execSQL("UPDATE " + TABLE_COMPLETED_TRANSFERS + " SET " + KEY_TRANSFER_ERROR + " = '" + encrypt("") + "';");
			db.execSQL("ALTER TABLE " + TABLE_COMPLETED_TRANSFERS + " ADD COLUMN " + KEY_TRANSFER_ORIGINAL_PATH + " TEXT;");
			db.execSQL("UPDATE " + TABLE_COMPLETED_TRANSFERS + " SET " + KEY_TRANSFER_ORIGINAL_PATH + " = '" + encrypt("") + "';");
			db.execSQL("ALTER TABLE " + TABLE_COMPLETED_TRANSFERS + " ADD COLUMN " + KEY_TRANSFER_PARENT_HANDLE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_COMPLETED_TRANSFERS + " SET " + KEY_TRANSFER_PARENT_HANDLE + " = '" + encrypt(INVALID_HANDLE + "") + "';");
		}

		if (oldVersion <= 58) {
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_TRANSFER_QUEUE_STATUS + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_TRANSFER_QUEUE_STATUS + " = '" + encrypt("false") + "';");

			db.execSQL(CREATE_SD_TRANSFERS_TABLE);
		}

        if (oldVersion <= 59) {
            db.execSQL(CREATE_BACKUP_TABLE);
        }

		if (oldVersion <= 60) {
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PASSCODE_LOCK_REQUIRE_TIME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PASSCODE_LOCK_REQUIRE_TIME
					+ " = '" + encrypt("" + (isPasscodeLockEnabled(db) ? REQUIRE_PASSCODE_IMMEDIATE : REQUIRE_PASSCODE_INVALID)) + "';");
		}

		if (oldVersion <= 61) {
			recreateAttributes(db, getAttributes(db));
			attributesAlreadyUpdated = true;
		}

		if (oldVersion <= 62) {
			if (!chatSettingsAlreadyUpdated) {
				recreateChatSettings(db, getChatSettingsFromDBv62(db));
				chatSettingsAlreadyUpdated = true;
			}

			recreatePreferences(db, getPreferencesFromDBv62(db));
			preferencesAlreadyUpdated = true;
		}

		if (oldVersion <= 63 && !preferencesAlreadyUpdated) {
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_FINGERPRINT_LOCK + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_FINGERPRINT_LOCK + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <= 64 && !preferencesAlreadyUpdated) {
			//KEY_CAM_SYNC_CHARGING and KEY_SMALL_GRID_CAMERA have been removed in DB v65
			recreatePreferences(db, getPreferences(db));
			preferencesAlreadyUpdated = true;
		}

		if (oldVersion <= 65 && !preferencesAlreadyUpdated) {
			//KEY_PREFERRED_SORT_CONTACTS has been removed in DB v66
			recreatePreferences(db, getPreferences(db));
			preferencesAlreadyUpdated = true;
		}

		if (oldVersion <= 66 && !attributesAlreadyUpdated) {
			//KEY_FILE_LOGGER_SDK and KEY_FILE_LOGGER_KARERE have been removed in DB v67
			recreateAttributes(db, getAttributes(db));
			attributesAlreadyUpdated = true;
		}

		this.db = db;
	}

	/**
	 * Drops the chat settings table if exists, creates the new one,
	 * and then sets the updated chat settings.
	 *
	 * @param db			Current DB.
	 * @param chatSettings	Chat Settings.
	 */
	private void recreateChatSettings(SQLiteDatabase db, ChatSettings chatSettings) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_SETTINGS);
		onCreate(db);

		if (chatSettings != null) {
			setChatSettings(db, chatSettings);
		}

		// Temporary fix to avoid wrong values in chat settings after upgrade.
		getChatSettings(db);
	}

	/**
	 * Drops the attributes table if exists, creates the new one,
	 * and then sets the updated attributes.
	 *
	 * @param db	Current DB.
	 * @param attr	Attributes.
	 */
	private void recreateAttributes(SQLiteDatabase db, MegaAttributes attr) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRIBUTES);
		onCreate(db);

		if (attr != null) {
			setAttributes(db, attr);
		}

		// Temporary fix to avoid wrong values in attributes after upgrade.
		getAttributes(db);
	}

	/**
	 * Drops the preferences table if exists, creates the new one,
	 * and then sets the updated preferences.
	 *
	 * @param db			Current DB.
	 * @param preferences	Preferences.
	 */
	private void recreatePreferences(SQLiteDatabase db, MegaPreferences preferences) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
		onCreate(db);

		if (preferences != null) {
			setPreferences(db, preferences);
		}

		// Temporary fix to avoid wrong values in preferences after upgrade.
		getPreferences(db);
	}

	public static String encrypt(String original) {
		if (original == null) {
			return null;
		}
		try {
			byte[] encrypted = aes_encrypt(getAesKey(),original.getBytes());
			return Base64.encodeToString(encrypted, Base64.DEFAULT);
		} catch (Exception e) {
            logError("Error encrypting DB field", e);
			e.printStackTrace();
			return null;
		}
	}

	private static byte[] getAesKey() {
		String key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC$G*(G($*GR*(#)*huio4h389$G";
		return Arrays.copyOfRange(key.getBytes(), 0, 32);
	}

	public void saveCredentials(UserCredentials userCredentials) {
		ContentValues values = new ContentValues();
        if (userCredentials.getEmail() != null){
        	values.put(KEY_EMAIL, encrypt(userCredentials.getEmail()));
        }
        if (userCredentials.getSession() != null){
           	values.put(KEY_SESSION, encrypt(userCredentials.getSession()));
        }
		if (userCredentials.getMyHandle() != null){
			values.put(KEY_MY_HANDLE, encrypt(userCredentials.getMyHandle()+""));
		}
        db.insert(TABLE_CREDENTIALS, null, values);
    }

    public void saveSyncRecord(SyncRecord record) {
        ContentValues values = new ContentValues();
        if (record.getLocalPath() != null) {
            values.put(KEY_SYNC_FILEPATH_ORI,encrypt(record.getLocalPath()));
        }
        if (record.getNewPath() != null) {
            values.put(KEY_SYNC_FILEPATH_NEW,encrypt(record.getNewPath()));
        }
        if (record.getOriginFingerprint() != null) {
            values.put(KEY_SYNC_FP_ORI,encrypt(record.getOriginFingerprint()));
        }
        if (record.getNewFingerprint() != null) {
            values.put(KEY_SYNC_FP_NEW,encrypt(record.getNewFingerprint()));
        }
        if (record.getFileName() != null) {
            values.put(KEY_SYNC_FILENAME,encrypt(record.getFileName()));
        }
        if (record.getNodeHandle() != null) {
            values.put(KEY_SYNC_HANDLE,encrypt(String.valueOf(record.getNodeHandle())));
        }
        if (record.getTimestamp() != null) {
            values.put(KEY_SYNC_TIMESTAMP,encrypt(String.valueOf(record.getTimestamp())));
        }
		values.put(KEY_SYNC_COPYONLY,encrypt(String.valueOf(record.isCopyOnly())));
		values.put(KEY_SYNC_SECONDARY,encrypt(String.valueOf(record.isSecondary())));
		if (record.getLongitude() != null) {
            values.put(KEY_SYNC_LONGITUDE,encrypt(String.valueOf(record.getLongitude())));
        }
        if (record.getLatitude() != null) {
            values.put(KEY_SYNC_LATITUDE,encrypt(String.valueOf(record.getLatitude())));
        }
        values.put(KEY_SYNC_STATE,record.getStatus());
        values.put(KEY_SYNC_TYPE,record.getType());
        db.insert(TABLE_SYNC_RECORDS,null,values);
    }

    public void updateVideoState(int state) {
        String sql = "UPDATE " + TABLE_SYNC_RECORDS + " SET " + KEY_SYNC_STATE + " = " + state + "  WHERE "
                + KEY_SYNC_TYPE + " = " + SyncRecordType.TYPE_VIDEO.getValue();
        db.execSQL(sql);
    }

    public boolean fileNameExists(String name,boolean isSecondary,int fileType) {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_FILENAME + " ='" + encrypt(name) + "' AND "
                + KEY_SYNC_SECONDARY + " = '" + encrypt(String.valueOf(isSecondary)) + "'";
        if (fileType != SyncRecordType.TYPE_ANY.getValue()) {
            selectQuery += " AND " + KEY_SYNC_TYPE + " = " + fileType;
        }
        try (Cursor cursor = db.rawQuery(selectQuery,null)) {
            return cursor != null && cursor.getCount() == 1;
        }
    }

    public boolean localPathExists(String localPath,boolean isSecondary,int fileType) {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_FILEPATH_ORI + " ='" + encrypt(localPath) + "' AND "
                + KEY_SYNC_SECONDARY + " = '" + encrypt(String.valueOf(isSecondary)) + "'";
        if (fileType != SyncRecordType.TYPE_ANY.getValue()) {
            selectQuery += " AND " + KEY_SYNC_TYPE + " = " + fileType;
        }
        try (Cursor cursor = db.rawQuery(selectQuery,null)) {
            return cursor != null && cursor.getCount() == 1;
        }
    }

    public SyncRecord recordExists(String originalFingerprint,boolean isSecondary,boolean isCopyOnly) {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_FP_ORI + " ='" + encrypt(originalFingerprint) + "' AND "
                + KEY_SYNC_SECONDARY + " = '" + encrypt(String.valueOf(isSecondary)) + "' AND "
                + KEY_SYNC_COPYONLY + " = '" + encrypt(String.valueOf(isCopyOnly)) + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)){
			if (cursor != null && cursor.moveToFirst()) {
				return extractSyncRecord(cursor);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return null;
    }

    public List<SyncRecord> findAllPendingSyncRecords() {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_STATE + " = " + SyncStatus.STATUS_PENDING.getValue();
		List<SyncRecord> records = new ArrayList<>();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SyncRecord record = extractSyncRecord(cursor);
					records.add(record);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return records;
    }

    public List<SyncRecord> findVideoSyncRecordsByState(int state) {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_STATE + " = " + state + " AND "
                + KEY_SYNC_TYPE + " = " + SyncRecordType.TYPE_VIDEO.getValue() ;
		List<SyncRecord> records = new ArrayList<>();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SyncRecord record = extractSyncRecord(cursor);
					records.add(record);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return records;
    }

    public void deleteAllSyncRecords(int type){
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS;
        if(type != SyncRecordType.TYPE_ANY.getValue()) {
            sql += " WHERE " + KEY_SYNC_TYPE + " = " + type;
        }
        db.execSQL(sql);
    }

    public void deleteAllSecondarySyncRecords(int type){
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS +" WHERE " + KEY_SYNC_SECONDARY + " ='" + encrypt("true") + "'";
        if(type != SyncRecordType.TYPE_ANY.getValue()) {
            sql += " AND " + KEY_SYNC_TYPE + " = " + type;
        }
        db.execSQL(sql);
    }

	public void deleteAllPrimarySyncRecords(int type) {
		String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + " WHERE " + KEY_SYNC_SECONDARY + " ='" + encrypt("false") + "'";
		if (type != SyncRecordType.TYPE_ANY.getValue()) {
			sql += " AND " + KEY_SYNC_TYPE + " = " + type;
		}
		db.execSQL(sql);
	}

    public void deleteVideoRecordsByState(int state){
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_STATE + " = " + state + " AND "
                + KEY_SYNC_TYPE + " = " + SyncRecordType.TYPE_VIDEO.getValue() ;
        db.execSQL(sql);
    }

	private SyncRecord extractSyncRecord(Cursor cursor) {
		String timestamp = decrypt(cursor.getString(5));
		String longitude = decrypt(cursor.getString(7));
		String latitude = decrypt(cursor.getString(8));
		String nodeHandle = decrypt(cursor.getString(11));
		return new SyncRecord(cursor.getInt(0),
				decrypt(cursor.getString(1)),
				decrypt(cursor.getString(2)),
				decrypt(cursor.getString(3)),
				decrypt(cursor.getString(4)),
				!TextUtils.isEmpty(timestamp) ? Long.valueOf(timestamp) : null,
				decrypt(cursor.getString(6)),
				!TextUtils.isEmpty(longitude) ? Float.valueOf(longitude) : null,
				!TextUtils.isEmpty(latitude) ? Float.valueOf(latitude) : null,
				cursor.getInt(9),
				cursor.getInt(10),
				!TextUtils.isEmpty(nodeHandle) ? Long.valueOf(nodeHandle) : null,
				Boolean.parseBoolean(decrypt(cursor.getString(12))),
				Boolean.parseBoolean(decrypt(cursor.getString(13))));
	}

    public SyncRecord findSyncRecordByLocalPath(String localPath,boolean isSecondary) {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_FILEPATH_ORI + " ='" + encrypt(localPath) + "' AND "
                + KEY_SYNC_SECONDARY + " ='" + encrypt(String.valueOf(isSecondary)) + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return extractSyncRecord(cursor);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return null;
    }

    public void deleteSyncRecordByPath(String path,boolean isSecondary) {
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + "  WHERE ("
                + KEY_SYNC_FILEPATH_ORI + " ='" + encrypt(path) + "' OR "
                + KEY_SYNC_FILEPATH_NEW + " ='" + encrypt(path) + "') AND "
                + KEY_SYNC_SECONDARY + " ='" + encrypt(String.valueOf(isSecondary)) + "'";
        db.execSQL(sql);
    }

    public void deleteSyncRecordByLocalPath(String localPath,boolean isSecondary) {
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + "  WHERE "
                + KEY_SYNC_FILEPATH_ORI + " ='" + encrypt(localPath) + "' AND "
                + KEY_SYNC_SECONDARY + " ='" + encrypt(String.valueOf(isSecondary)) + "'";
        db.execSQL(sql);
    }

    public void deleteSyncRecordByNewPath(String newPath) {
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + "  WHERE " + KEY_SYNC_FILEPATH_NEW + " ='" + encrypt(newPath) + "'";
        db.execSQL(sql);
    }

    public void deleteSyncRecordByFileName(String fileName) {
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + "  WHERE "
                + KEY_SYNC_FILENAME + " = '" + encrypt(fileName) + "'" + " OR "
                + KEY_SYNC_FILEPATH_ORI +  " LIKE '%" + encrypt(fileName) + "'";
        db.execSQL(sql);
    }

    public void deleteSyncRecordByFingerprint(String oriFingerprint,String newFingerprint,boolean isSecondary) {
        String sql = "DELETE FROM " + TABLE_SYNC_RECORDS + "  WHERE "
                + KEY_SYNC_FP_ORI + " = '" + encrypt(oriFingerprint) + "' OR "
                + KEY_SYNC_FP_NEW + " = '" + encrypt(newFingerprint) + "' AND "
                + KEY_SYNC_SECONDARY + " ='" + encrypt(String.valueOf(isSecondary)) + "'";
        db.execSQL(sql);
    }

    public void updateSyncRecordStatusByLocalPath(int status,String localPath,boolean isSecondary) {
        String sql = "UPDATE " + TABLE_SYNC_RECORDS + " SET " + KEY_SYNC_STATE + " = " + status + "  WHERE "
                + KEY_SYNC_FILEPATH_ORI + " = '" + encrypt(localPath) + "' AND "
                + KEY_SYNC_SECONDARY + " ='" + encrypt(String.valueOf(isSecondary)) + "'";
        db.execSQL(sql);
    }

    public SyncRecord findSyncRecordByNewPath(String newPath) {
        String selectQuery = "SELECT * FROM " + TABLE_SYNC_RECORDS + " WHERE "
                + KEY_SYNC_FILEPATH_NEW + " ='" + encrypt(newPath) + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return extractSyncRecord(cursor);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return null;
    }

    public boolean shouldClearCamsyncRecords() {
        String selectQuery = "SELECT " + KEY_SHOULD_CLEAR_CAMSYNC_RECORDS + " FROM " + TABLE_PREFERENCES;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String should = cursor.getString(cursor.getColumnIndex(KEY_SHOULD_CLEAR_CAMSYNC_RECORDS));
				should = decrypt(should);
				if (TextUtils.isEmpty(should)) {
					return false;
				} else {
					return Boolean.valueOf(should);
				}
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return false;
    }

    public void saveShouldClearCamsyncRecords(boolean should) {
        String sql = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SHOULD_CLEAR_CAMSYNC_RECORDS +" = '" + encrypt(String.valueOf(should)) + "'";
        db.execSQL(sql);
    }

    public Long findMaxTimestamp(Boolean isSecondary,int type) {
        String selectQuery = "SELECT " + KEY_SYNC_TIMESTAMP + " FROM " + TABLE_SYNC_RECORDS + "  WHERE "
                + KEY_SYNC_SECONDARY + " = '" + encrypt(String.valueOf(isSecondary)) + "' AND "
                + KEY_SYNC_TYPE + " = " + type;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				List<Long> timestamps = new ArrayList<>(cursor.getCount());
				do {
					String timestamp = decrypt(cursor.getString(0));
					if (timestamp == null) {
						timestamps.add(0L);
					} else {
						timestamps.add(Long.valueOf(timestamp));
					}
				} while (cursor.moveToNext());

				if (timestamps.isEmpty()) {
					return null;
				}
				Collections.sort(timestamps, new Comparator<Long>() {

					@Override
					public int compare(Long o1, Long o2) {
						if (o1.equals(o2)) {
							return 0;
						}
						return (o1 > o2) ? -1 : 1;
					}
				});
				return timestamps.get(0);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return null;
    }

    public void setCameraUploadVideoQuality(int quality){
        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_UPLOAD_VIDEO_QUALITY + "= '" + encrypt(String.valueOf(quality)) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_UPLOAD_VIDEO_QUALITY, encrypt(String.valueOf(quality)));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

    public void setConversionOnCharging (boolean onCharging){
        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CONVERSION_ON_CHARGING + "= '" + encrypt(String.valueOf(onCharging)) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CONVERSION_ON_CHARGING, encrypt(String.valueOf(onCharging)));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

    public void setChargingOnSize (int size){
        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CHARGING_ON_SIZE + "= '" + encrypt(String.valueOf(size)) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CHARGING_ON_SIZE, encrypt(String.valueOf(size)));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

    public void setRemoveGPS (boolean removeGPS){
        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_REMOVE_GPS + "= '" + encrypt(String.valueOf(removeGPS)) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_REMOVE_GPS, encrypt(String.valueOf(removeGPS)));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

	public void saveEphemeral(EphemeralCredentials ephemeralCredentials) {
		ContentValues values = new ContentValues();
		if (ephemeralCredentials.getEmail() != null){
			values.put(KEY_EMAIL, encrypt(ephemeralCredentials.getEmail()));
		}
		if (ephemeralCredentials.getPassword() != null){
			values.put(KEY_PASSWORD, encrypt(ephemeralCredentials.getPassword()));
		}
		if (ephemeralCredentials.getSession() != null){
			values.put(KEY_SESSION, encrypt(ephemeralCredentials.getSession()));
		}
		if (ephemeralCredentials.getFirstName() != null){
			values.put(KEY_FIRST_NAME, encrypt(ephemeralCredentials.getFirstName()));
		}
		if (ephemeralCredentials.getLastName() != null){
			values.put(KEY_LAST_NAME, encrypt(ephemeralCredentials.getLastName()));
		}
		db.insert(TABLE_EPHEMERAL, null, values);
	}

	public void saveMyEmail(String email) {
        logDebug("saveEmail: " + email);
		String selectQuery = "SELECT * FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_CREDENTIALS_TABLE = "UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_EMAIL + "= '" + encrypt(email) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_CREDENTIALS_TABLE);
			} else {
				values.put(KEY_EMAIL, encrypt(email));
				db.insert(TABLE_CREDENTIALS, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void saveMyFirstName(String firstName) {

		String selectQuery = "SELECT * FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_CREDENTIALS_TABLE = "UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_FIRST_NAME + "= '" + encrypt(firstName) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_CREDENTIALS_TABLE);
			} else {
				values.put(KEY_FIRST_NAME, encrypt(firstName));
				db.insert(TABLE_CREDENTIALS, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void saveMyLastName(String lastName) {
		String selectQuery = "SELECT * FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_CREDENTIALS_TABLE = "UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_LAST_NAME + "= '" + encrypt(lastName) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_CREDENTIALS_TABLE);
			} else {
				values.put(KEY_LAST_NAME, encrypt(lastName));
				db.insert(TABLE_CREDENTIALS, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public String getMyEmail() {
		String selectQuery = "SELECT "+KEY_EMAIL+" FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		String email = null;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				email = decrypt(cursor.getString(0));
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return email;
	}

	public static String decrypt(String encodedString) {
		if (encodedString == null) {
			return null;
		}
		try {
			byte[] encoded = Base64.decode(encodedString, Base64.DEFAULT);
			byte[] original = aes_decrypt(getAesKey(), encoded);
			return new String(original);
		} catch (Exception e) {
            logError("Error decrypting DB field", e);
            e.printStackTrace();
			return null;
		}
	}

	public UserCredentials getCredentials(){
		UserCredentials userCredentials = null;
		String selectQuery = "SELECT  * FROM " + TABLE_CREDENTIALS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)){
			//get the credential of last login
			if (cursor != null && cursor.moveToLast()) {
				int id = Integer.parseInt(cursor.getString(0));
				String email = decrypt(cursor.getString(1));
				String session = decrypt(cursor.getString(2));
				String firstName = decrypt(cursor.getString(3));
				String lastName = decrypt(cursor.getString(4));
				String myHandle = decrypt(cursor.getString(5));
				userCredentials = new UserCredentials(email, session, firstName, lastName, myHandle);
			}
		} catch (SQLiteException e){
			if (db != null){
				onCreate(db);
			}
		} catch (Exception e) {
			logError("Error decrypting DB field", e);
		}
        return userCredentials;
	}

    public void batchInsertMegaContacts(List<MegaContactGetter.MegaContact> contacts) {
        if (contacts == null || contacts.size() == 0) {
            logWarning("Empty MEGA contacts list.");
            return;
        }
        logDebug("Contacts size is: " + contacts.size());
        db.beginTransaction();
        try {
            ContentValues values;
            for (MegaContactGetter.MegaContact contact : contacts) {
                values = new ContentValues();
                values.put(KEY_MEGA_CONTACTS_ID, encrypt(contact.getId()));
                values.put(KEY_MEGA_CONTACTS_HANDLE, encrypt(String.valueOf(contact.getHandle())));
                values.put(KEY_MEGA_CONTACTS_LOCAL_NAME, encrypt(contact.getLocalName()));
                values.put(KEY_MEGA_CONTACTS_EMAIL, encrypt(contact.getEmail()));
                values.put(KEY_MEGA_CONTACTS_PHONE_NUMBER, encrypt(contact.getNormalizedPhoneNumber()));

                db.insert(TABLE_MEGA_CONTACTS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<MegaContactGetter.MegaContact> getMegaContacts() {
        String sql = "SELECT * FROM " + TABLE_MEGA_CONTACTS;
        ArrayList<MegaContactGetter.MegaContact> contacts = new ArrayList<>();
		try (Cursor cursor = db.rawQuery(sql, null)) {
			if (cursor != null) {
				MegaContactGetter.MegaContact contact;
				while (cursor.moveToNext()) {
					contact = new MegaContactGetter.MegaContact();
					String id = cursor.getString(cursor.getColumnIndex(KEY_MEGA_CONTACTS_ID));
					contact.setId(decrypt(id));
					String handle = cursor.getString(cursor.getColumnIndex(KEY_MEGA_CONTACTS_HANDLE));
					contact.setHandle(Long.valueOf(decrypt(handle)));
					String localName = cursor.getString(cursor.getColumnIndex(KEY_MEGA_CONTACTS_LOCAL_NAME));
					contact.setLocalName(decrypt(localName));
					String email = cursor.getString(cursor.getColumnIndex(KEY_MEGA_CONTACTS_EMAIL));
					contact.setEmail(decrypt(email));
					String phoneNumber = cursor.getString(cursor.getColumnIndex(KEY_MEGA_CONTACTS_PHONE_NUMBER));
					contact.setNormalizedPhoneNumber(decrypt(phoneNumber));
					contacts.add(contact);
				}
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return contacts;
    }

    public void clearMegaContacts() {
        logDebug("delete table " + TABLE_MEGA_CONTACTS);
        db.execSQL("DELETE FROM " + TABLE_MEGA_CONTACTS);
    }

    public EphemeralCredentials getEphemeral(){
        EphemeralCredentials ephemeralCredentials = null;
        String selectQuery = "SELECT  * FROM " + TABLE_EPHEMERAL;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int id = Integer.parseInt(cursor.getString(0));
				String email = decrypt(cursor.getString(1));
				String password = decrypt(cursor.getString(2));
				String session = decrypt(cursor.getString(3));
				String firstName = decrypt(cursor.getString(4));
				String lastName = decrypt(cursor.getString(5));
				ephemeralCredentials = new EphemeralCredentials(email, password, session, firstName, lastName);
			}
		} catch (SQLiteException e) {
			if (db != null) {
				onCreate(db);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return ephemeralCredentials;
    }

	/**
	 * Sets preferences.
	 *
	 * @param db    Current DB.
	 * @param prefs Preferences.
	 */
	private void setPreferences (SQLiteDatabase db, MegaPreferences prefs){
        ContentValues values = new ContentValues();
        values.put(KEY_FIRST_LOGIN, encrypt(prefs.getFirstTime()));
        values.put(KEY_CAM_SYNC_WIFI, encrypt(prefs.getCamSyncWifi()));
        values.put(KEY_CAM_SYNC_ENABLED, encrypt(prefs.getCamSyncEnabled()));
        values.put(KEY_CAM_SYNC_HANDLE, encrypt(prefs.getCamSyncHandle()));
        values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(prefs.getCamSyncLocalPath()));
        values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(prefs.getCamSyncFileUpload()));
        values.put(KEY_PASSCODE_LOCK_ENABLED, encrypt(prefs.getPasscodeLockEnabled()));
        values.put(KEY_PASSCODE_LOCK_CODE, encrypt(prefs.getPasscodeLockCode()));
        values.put(KEY_STORAGE_ASK_ALWAYS, encrypt(prefs.getStorageAskAlways()));
        values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(prefs.getStorageDownloadLocation()));
        values.put(KEY_CAM_SYNC_TIMESTAMP, encrypt(prefs.getCamSyncTimeStamp()));
        values.put(KEY_CAM_VIDEO_SYNC_TIMESTAMP, encrypt(prefs.getCamVideoSyncTimeStamp()));
        values.put(KEY_LAST_UPLOAD_FOLDER, encrypt(prefs.getLastFolderUpload()));
        values.put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(prefs.getLastFolderCloud()));
        values.put(KEY_SEC_FOLDER_ENABLED, encrypt(prefs.getSecondaryMediaFolderEnabled()));
        values.put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(prefs.getLocalPathSecondaryFolder()));
        values.put(KEY_SEC_FOLDER_HANDLE, encrypt(prefs.getMegaHandleSecondaryFolder()));
        values.put(KEY_SEC_SYNC_TIMESTAMP, encrypt(prefs.getSecSyncTimeStamp()));
        values.put(KEY_SEC_VIDEO_SYNC_TIMESTAMP, encrypt(prefs.getSecVideoSyncTimeStamp()));
        values.put(KEY_STORAGE_ADVANCED_DEVICES, encrypt(prefs.getStorageAdvancedDevices()));
        values.put(KEY_PREFERRED_VIEW_LIST, encrypt(prefs.getPreferredViewList()));
        values.put(KEY_PREFERRED_VIEW_LIST_CAMERA, encrypt(prefs.getPreferredViewListCameraUploads()));
        values.put(KEY_URI_EXTERNAL_SD_CARD, encrypt(prefs.getUriExternalSDCard()));
        values.put(KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD, encrypt(prefs.getCameraFolderExternalSDCard()));
        values.put(KEY_PASSCODE_LOCK_TYPE, encrypt(prefs.getPasscodeLockType()));
		values.put(KEY_PREFERRED_SORT_CLOUD, encrypt(prefs.getPreferredSortCloud()));
		values.put(KEY_PREFERRED_SORT_CAMERA_UPLOAD, encrypt(prefs.getPreferredSortCameraUpload()));
		values.put(KEY_PREFERRED_SORT_OTHERS, encrypt(prefs.getPreferredSortOthers()));
		values.put(KEY_FIRST_LOGIN_CHAT, encrypt(prefs.getFirstTimeChat()));
		values.put(KEY_REMOVE_GPS, encrypt(prefs.getRemoveGPS()));
		values.put(KEY_KEEP_FILE_NAMES, encrypt(prefs.getKeepFileNames()));
		values.put(KEY_AUTO_PLAY, encrypt(prefs.isAutoPlayEnabled() + ""));
		values.put(KEY_UPLOAD_VIDEO_QUALITY, encrypt(prefs.getUploadVideoQuality()));
		values.put(KEY_CONVERSION_ON_CHARGING, encrypt(prefs.getConversionOnCharging()));
		values.put(KEY_CHARGING_ON_SIZE, encrypt(prefs.getChargingOnSize()));
		values.put(KEY_SHOULD_CLEAR_CAMSYNC_RECORDS, encrypt(prefs.getShouldClearCameraSyncRecords()));
		values.put(KEY_SHOW_INVITE_BANNER, encrypt(prefs.getShowInviteBanner()));
		values.put(KEY_SD_CARD_URI, encrypt(prefs.getSdCardUri()));
		values.put(KEY_ASK_FOR_DISPLAY_OVER, encrypt(prefs.getAskForDisplayOver()));
		values.put(KEY_ASK_SET_DOWNLOAD_LOCATION, encrypt(prefs.getAskForSetDownloadLocation()));
		values.put(KEY_URI_MEDIA_EXTERNAL_SD_CARD, encrypt(prefs.getMediaSDCardUri()));
		values.put(KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD, encrypt(prefs.getIsMediaOnSDCard()));
		values.put(KEY_PASSCODE_LOCK_REQUIRE_TIME, encrypt(prefs.getPasscodeLockRequireTime()));
		values.put(KEY_FINGERPRINT_LOCK, encrypt(prefs.getFingerprintLock()));

        db.insert(TABLE_PREFERENCES, null, values);
	}

	public boolean shouldAskForDisplayOver() {
        boolean should = true;
        String text = getStringValue(TABLE_PREFERENCES, KEY_ASK_FOR_DISPLAY_OVER, "");
        if (!TextUtils.isEmpty(text)) {
            should = Boolean.parseBoolean(text);
        }
        return should;
    }

    public void dontAskForDisplayOver() {
        db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_ASK_FOR_DISPLAY_OVER + " = '" + encrypt("false") + "';");
    }

	/**
	 * Gets preferences from the DB v62 (previous to add four available video qualities).
	 *
	 * @param db Current DB.
	 * @return Preferences.
	 */
	private MegaPreferences getPreferencesFromDBv62(SQLiteDatabase db) {
		logDebug("getPreferencesFromDBv62");
		MegaPreferences preferences = getPreferences(db);

		if (preferences != null) {
			String uploadVideoQuality = preferences.getUploadVideoQuality();

			if (!isTextEmpty(uploadVideoQuality)
					&& Integer.parseInt(uploadVideoQuality) == OLD_VIDEO_QUALITY_ORIGINAL) {
				preferences.setUploadVideoQuality(String.valueOf(VIDEO_QUALITY_ORIGINAL));
			}
		}

		return preferences;
	}

	/**
	 * Gets preferences.
	 *
	 * @return Preferences.
	 */
	public MegaPreferences getPreferences(){
        return getPreferences(db);
	}

	/**
	 * Gets preferences.
	 *
	 * @param db Current DB.
	 * @return Preferences.
	 */
	private MegaPreferences getPreferences(SQLiteDatabase db) {
		MegaPreferences prefs = null;
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;

		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String firstTime = decrypt(cursor.getString(getColumnIndex(cursor, KEY_FIRST_LOGIN)));
				String camSyncEnabled = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_ENABLED)));
				String camSyncHandle = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_HANDLE)));
				String camSyncLocalPath = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_LOCAL_PATH)));
				String wifi = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_WIFI)));
				String fileUpload = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_FILE_UPLOAD)));
				String pinLockEnabled = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_ENABLED)));
				String pinLockCode = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_CODE)));
				String askAlways = decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_ASK_ALWAYS)));
				String downloadLocation = decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_DOWNLOAD_LOCATION)));
				String camSyncTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_TIMESTAMP)));
				String lastFolderUpload = decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_UPLOAD_FOLDER)));
				String lastFolderCloud = decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_CLOUD_FOLDER_HANDLE)));
				String secondaryFolderEnabled = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_FOLDER_ENABLED)));
				String secondaryPath = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_FOLDER_LOCAL_PATH)));
				String secondaryHandle = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_FOLDER_HANDLE)));
				String secSyncTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_SYNC_TIMESTAMP)));
				String keepFileNames = decrypt(cursor.getString(getColumnIndex(cursor, KEY_KEEP_FILE_NAMES)));
				String storageAdvancedDevices = decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_ADVANCED_DEVICES)));
				String preferredViewList = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_VIEW_LIST)));
				String preferredViewListCamera = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_VIEW_LIST_CAMERA)));
				String uriExternalSDCard = decrypt(cursor.getString(getColumnIndex(cursor, KEY_URI_EXTERNAL_SD_CARD)));
				String cameraFolderExternalSDCard = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD)));
				String pinLockType = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_TYPE)));
				String preferredSortCloud = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_SORT_CLOUD)));
				String preferredSortOthers = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_SORT_OTHERS)));
				String firstTimeChat = decrypt(cursor.getString(getColumnIndex(cursor, KEY_FIRST_LOGIN_CHAT)));
				String isAutoPlayEnabled = decrypt(cursor.getString(getColumnIndex(cursor, KEY_AUTO_PLAY)));
				String uploadVideoQuality = decrypt(cursor.getString(getColumnIndex(cursor, KEY_UPLOAD_VIDEO_QUALITY)));
				String conversionOnCharging = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CONVERSION_ON_CHARGING)));
				String chargingOnSize = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CHARGING_ON_SIZE)));
				String shouldClearCameraSyncRecords = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOULD_CLEAR_CAMSYNC_RECORDS)));
				String camVideoSyncTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_VIDEO_SYNC_TIMESTAMP)));
				String secVideoSyncTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_VIDEO_SYNC_TIMESTAMP)));
				String removeGPS = decrypt(cursor.getString(getColumnIndex(cursor, KEY_REMOVE_GPS)));
				String closeInviteBanner = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_INVITE_BANNER)));
				String preferredSortCameraUpload = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_SORT_CAMERA_UPLOAD)));
				String sdCardUri = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SD_CARD_URI)));
				String askForDisplayOver = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ASK_FOR_DISPLAY_OVER)));
				String askForSetDownloadLocation = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ASK_SET_DOWNLOAD_LOCATION)));
				String mediaSDCardUri = decrypt(cursor.getString(getColumnIndex(cursor, KEY_URI_MEDIA_EXTERNAL_SD_CARD)));
				String isMediaOnSDCard = decrypt(cursor.getString(getColumnIndex(cursor, KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD)));
				String passcodeLockRequireTime = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_REQUIRE_TIME)));
				String fingerprintLock = cursor.getColumnIndex(KEY_FINGERPRINT_LOCK) != INVALID_VALUE
						? decrypt(cursor.getString(getColumnIndex(cursor, KEY_FINGERPRINT_LOCK)))
						: "false";

				prefs = new MegaPreferences(firstTime, wifi, camSyncEnabled, camSyncHandle,
						camSyncLocalPath, fileUpload, camSyncTimeStamp, pinLockEnabled,
						pinLockCode, askAlways, downloadLocation, lastFolderUpload,
						lastFolderCloud, secondaryFolderEnabled, secondaryPath, secondaryHandle,
						secSyncTimeStamp, keepFileNames, storageAdvancedDevices, preferredViewList,
						preferredViewListCamera, uriExternalSDCard, cameraFolderExternalSDCard,
						pinLockType, preferredSortCloud, preferredSortOthers, firstTimeChat,
						uploadVideoQuality, conversionOnCharging, chargingOnSize,
						shouldClearCameraSyncRecords, camVideoSyncTimeStamp, secVideoSyncTimeStamp,
						isAutoPlayEnabled, removeGPS, closeInviteBanner, preferredSortCameraUpload,
						sdCardUri, askForDisplayOver, askForSetDownloadLocation, mediaSDCardUri,
						isMediaOnSDCard, passcodeLockRequireTime, fingerprintLock);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}

		return prefs;
	}

	/**
	 * Get chat settings from the DB v52 (previous to remove the setting to enable/disable the chat).
	 * KEY_CHAT_ENABLED and KEY_CHAT_STATUS have been removed in DB v53.
	 *
	 * @return Chat settings.
	 */
	private ChatSettings getChatSettingsFromDBv52(SQLiteDatabase db){
        logDebug("getChatSettings");
		ChatSettings chatSettings = null;

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int id = Integer.parseInt(cursor.getString(0));
				String enabled = decrypt(cursor.getString(1));
				String notificationSound = decrypt(cursor.getString(3));
				String vibrationEnabled = decrypt(cursor.getString(4));
				String chatStatus = decrypt(cursor.getString(5));
				String sendOriginalAttachments = decrypt(cursor.getString(6));
				String videoQuality = Boolean.parseBoolean(sendOriginalAttachments)
						? VIDEO_QUALITY_ORIGINAL + ""
						: VIDEO_QUALITY_MEDIUM + "";

				chatSettings = new ChatSettings(notificationSound, vibrationEnabled, videoQuality);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return chatSettings;
	}

	/**
	 * Get chat settings from the DB v62 (previous to remove the setting to enable/disable
	 * the send original attachments and to add four available video qualities).
	 * KEY_CHAT_SEND_ORIGINALS has been removed in DB v63.
	 *
	 * @return Chat settings.
	 */
	private ChatSettings getChatSettingsFromDBv62(SQLiteDatabase db){
		logDebug("getChatSettings");
		ChatSettings chatSettings = null;

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String notificationSound = decrypt(cursor.getString(2));
				String vibrationEnabled = decrypt(cursor.getString(3));
				String sendOriginalAttachments = decrypt(cursor.getString(4));
				String videoQuality = Boolean.parseBoolean(sendOriginalAttachments)
						? VIDEO_QUALITY_ORIGINAL + ""
						: VIDEO_QUALITY_MEDIUM + "";

				chatSettings = new ChatSettings(notificationSound, vibrationEnabled, videoQuality);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return chatSettings;
	}

	/**
	 * Get chat settings from the current DB.
	 * @return Chat settings.
	 */
	public ChatSettings getChatSettings() {
		return getChatSettings(db);
	}

	/**
	 * Get chat settings from the current DB.
	 *
	 * @param db Current DB.
	 * @return Chat settings.
	 */
	private ChatSettings getChatSettings(SQLiteDatabase db) {
		logDebug("getChatSettings");
		ChatSettings chatSettings = null;

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String notificationSound = decrypt(cursor.getString(2));
				String vibrationEnabled = decrypt(cursor.getString(3));
				String videoQuality = decrypt(cursor.getString(4));
				chatSettings = new ChatSettings(notificationSound, vibrationEnabled, videoQuality);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return chatSettings;
	}

	/**
	 * Save chat settings in the current DB.
	 * @param chatSettings Chat settings to save.
	 */
	public void setChatSettings(ChatSettings chatSettings){
		setChatSettings(db, chatSettings);
	}

	/**
	 * Save chat settings in the DB.
	 * @param db DB object to save the settings.
	 * @param chatSettings Chat settings to save.
	 */
	private void setChatSettings(SQLiteDatabase db, ChatSettings chatSettings) {
		if (chatSettings == null) {
			logError("Error: Chat settings are null");
			return;
		}

		db.execSQL("DELETE FROM " + TABLE_CHAT_SETTINGS);
		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_NOTIFICATIONS_ENABLED, "");
		values.put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(chatSettings.getNotificationsSound()));
		values.put(KEY_CHAT_VIBRATION_ENABLED, encrypt(chatSettings.getVibrationEnabled()));
		values.put(KEY_CHAT_VIDEO_QUALITY, encrypt(chatSettings.getVideoQuality()));

		db.insert(TABLE_CHAT_SETTINGS, null, values);
	}

	/**
	 * Sets the chat video quality value.
	 * There are four possible values for this setting: VIDEO_QUALITY_ORIGINAL, VIDEO_QUALITY_HIGH,
	 * VIDEO_QUALITY_MEDIUM or VIDEO_QUALITY_LOW.
	 *
	 * @param chatVideoQuality The new chat video quality.
	 */
	public void setChatVideoQuality(int chatVideoQuality){
        logDebug("setChatVideoQuality");
        setIntValue(TABLE_CHAT_SETTINGS, KEY_CHAT_VIDEO_QUALITY, chatVideoQuality);
	}

	/**
	 * Gets the chat video quality value.
	 *
	 * @return The chat video quality.
	 */
	public int getChatVideoQuality(){
		logDebug("getChatVideoQuality");
		return getIntValue(TABLE_CHAT_SETTINGS, KEY_CHAT_VIDEO_QUALITY, VIDEO_QUALITY_MEDIUM);
	}

	public void setNotificationSoundChat(String sound){
		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_SOUND_NOTIFICATIONS + "= '" + encrypt(sound) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(sound));
				db.insert(TABLE_CHAT_SETTINGS, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setVibrationEnabledChat(String enabled){
		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_VIBRATION_ENABLED + "= '" + encrypt(enabled) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CHAT_VIBRATION_ENABLED, encrypt(enabled));
				db.insert(TABLE_CHAT_SETTINGS, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setChatItemPreferences(ChatItemPreferences chatPrefs){
		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_HANDLE, encrypt(chatPrefs.getChatHandle()));
		values.put(KEY_CHAT_ITEM_NOTIFICATIONS, "");
		values.put(KEY_CHAT_ITEM_RINGTONE, "");
		values.put(KEY_CHAT_ITEM_SOUND_NOTIFICATIONS, "");
		values.put(KEY_CHAT_ITEM_WRITTEN_TEXT, encrypt(chatPrefs.getWrittenText()));
		values.put(KEY_CHAT_ITEM_EDITED_MSG_ID, encrypt(chatPrefs.getEditedMsgId()));
		db.insert(TABLE_CHAT_ITEMS, null, values);
	}

	public int setWrittenTextItem(String handle, String text, String editedMsgId){
        logDebug("setWrittenTextItem: "+ text + " " + handle);

		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_ITEM_WRITTEN_TEXT, encrypt(text));
		values.put(KEY_CHAT_ITEM_EDITED_MSG_ID, !isTextEmpty(editedMsgId) ? encrypt(editedMsgId) : "");

		return db.update(TABLE_CHAT_ITEMS, values, KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'", null);
	}

	public ChatItemPreferences findChatPreferencesByHandle (String handle){
        logDebug("findChatPreferencesByHandle: " + handle);
		ChatItemPreferences prefs = null;

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_ITEMS + " WHERE " + KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'";
        logDebug("QUERY: " + selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int id = Integer.parseInt(cursor.getString(0));
				String chatHandle = decrypt(cursor.getString(1));
				String writtenText = decrypt(cursor.getString(5));
				String editedMsg = decrypt(cursor.getString(6));
				prefs = !isTextEmpty(editedMsg) ? new ChatItemPreferences(chatHandle, writtenText, editedMsg) :
						new ChatItemPreferences(chatHandle, writtenText);
				return prefs;
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
	}

	public String areNotificationsEnabled (String handle){

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_ITEMS + " WHERE " + KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'";
		String result = NOTIFICATIONS_ENABLED;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				result = decrypt(cursor.getString(2));
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return result;
	}

	/**
	 * Deletes the oldest completed transfer.
	 */
	private void deleteOldestTransfer() {
		ArrayList<AndroidCompletedTransfer> completedTransfers = getCompletedTransfers();
		if (!completedTransfers.isEmpty()) {
			deleteTransfer(completedTransfers.get(0).getId());
		}
	}

	/**
	 * Deletes a completed transfer.
	 *
	 * @param id	the identifier of the transfer to delete
	 */
	public void deleteTransfer(long id) {
		db.delete(TABLE_COMPLETED_TRANSFERS, KEY_ID + "=" + id, null);
	}

	/**
	 * Gets a completed transfer.
	 *
	 * @param id	the identifier of the transfer to get
	 * @return The completed transfer which has the id value as identifier.
	 */
	public AndroidCompletedTransfer getcompletedTransfer(long id) {
		String selectQuery = "SELECT * FROM " + TABLE_COMPLETED_TRANSFERS + " WHERE " + KEY_ID + " = '" + id + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return extractAndroidCompletedTransfer(cursor);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
	}

	/**
	 * Extracts a completed transfer of a row.
	 *
	 * @param cursor	Cursor from which the data should be extracted.
	 * @return The extracted completed transfer.
	 */
	private AndroidCompletedTransfer extractAndroidCompletedTransfer(Cursor cursor) {
		long id = Integer.parseInt(cursor.getString(0));
		String filename = decrypt(cursor.getString(1));
		String type = decrypt(cursor.getString(2));
		int typeInt = Integer.parseInt(type);
		String state = decrypt(cursor.getString(3));
		int stateInt = Integer.parseInt(state);
		String size = decrypt(cursor.getString(4));
		String nodeHandle = decrypt(cursor.getString(5));
		String path = decrypt(cursor.getString(6));
		boolean offline = Boolean.parseBoolean(decrypt(cursor.getString(7)));
		long timeStamp = Long.parseLong(decrypt(cursor.getString(8)));
		String error = decrypt(cursor.getString(9));
		String originalPath = decrypt(cursor.getString(10));
		long parentHandle = Long.parseLong(decrypt(cursor.getString(11)));

		return new AndroidCompletedTransfer(id, filename, typeInt, stateInt, size, nodeHandle, path,
				offline, timeStamp, error, originalPath, parentHandle);
	}

	public long setCompletedTransfer(AndroidCompletedTransfer transfer){
		ContentValues values = new ContentValues();
		values.put(KEY_TRANSFER_FILENAME, encrypt(transfer.getFileName()));
		values.put(KEY_TRANSFER_TYPE, encrypt(transfer.getType()+""));
		values.put(KEY_TRANSFER_STATE, encrypt(transfer.getState()+""));
		values.put(KEY_TRANSFER_SIZE, encrypt(transfer.getSize()));
		values.put(KEY_TRANSFER_HANDLE, encrypt(transfer.getNodeHandle()));
		values.put(KEY_TRANSFER_PATH, encrypt(transfer.getPath()));
		values.put(KEY_TRANSFER_OFFLINE, encrypt(transfer.getIsOfflineFile() + ""));
		values.put(KEY_TRANSFER_TIMESTAMP, encrypt(transfer.getTimeStamp() + ""));
		values.put(KEY_TRANSFER_ERROR, encrypt(transfer.getError()));
		values.put(KEY_TRANSFER_ORIGINAL_PATH, encrypt(transfer.getOriginalPath()));
		values.put(KEY_TRANSFER_PARENT_HANDLE, encrypt(transfer.getParentHandle() + ""));

		long id = db.insert(TABLE_COMPLETED_TRANSFERS, null, values);

		if (DatabaseUtils.queryNumEntries(db, TABLE_COMPLETED_TRANSFERS) > MAX_TRANSFERS) {
			deleteOldestTransfer();
		}

		return id;
	}

	/**
	 * Checks if a completed transfer exists before add it to DB.
	 * If so, does nothing. If not, adds the transfer to the DB.
	 *
	 * @param transfer The transfer to check and add.
	 */
	public void setCompletedTransferWithCheck(AndroidCompletedTransfer transfer){
		if (alreadyExistsAsCompletedTransfer(transfer)) {
			return;
		}

		setCompletedTransfer(transfer);
	}

	/**
	 * Checks if a completed transfer exists.
	 *
	 * @param transfer The completed transfer to check.
	 * @return True if the transfer already exists, false otherwise.
	 */
	private boolean alreadyExistsAsCompletedTransfer(AndroidCompletedTransfer transfer) {
		String selectQuery = "SELECT * FROM " + TABLE_COMPLETED_TRANSFERS
				+ " WHERE " + KEY_TRANSFER_FILENAME + " = '" + encrypt(transfer.getFileName())
				+ "' AND " + KEY_TRANSFER_TYPE + " = '" + encrypt(transfer.getType() + "")
				+ "' AND " + KEY_TRANSFER_STATE + " = '" + encrypt(transfer.getState() + "")
				+ "' AND " + KEY_TRANSFER_SIZE + " = '" + encrypt(transfer.getSize())
				+ "' AND " + KEY_TRANSFER_HANDLE + " = '" + encrypt(transfer.getNodeHandle())
				+ "' AND " + KEY_TRANSFER_PATH + " = '" + encrypt(transfer.getPath())
				+ "' AND " + KEY_TRANSFER_OFFLINE + " = '" + encrypt(transfer.getIsOfflineFile() + "")
				+ "' AND " + KEY_TRANSFER_ERROR + " = '" + encrypt(transfer.getError())
				+ "' AND " + KEY_TRANSFER_ORIGINAL_PATH + " = '" + encrypt(transfer.getOriginalPath())
				+ "' AND " + KEY_TRANSFER_PARENT_HANDLE + " = '" + encrypt(transfer.getParentHandle() + "") + "'";

		boolean isExist = false;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			isExist = cursor != null && cursor.getCount() > 0;
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return isExist;
	}

	public void emptyCompletedTransfers(){
		db.delete(TABLE_COMPLETED_TRANSFERS, null,null);
	}

    /**
     * Gets the completed transfers.
     *
     * @return The list with the completed transfers.
     */
	public ArrayList<AndroidCompletedTransfer> getCompletedTransfers(){
		String selectQuery = "SELECT * FROM " + TABLE_COMPLETED_TRANSFERS;
		ArrayList<AndroidCompletedTransfer> transfers =  getCompletedTransfers(selectQuery);
		Collections.sort(transfers, (transfer1, transfer2) -> Long.compare(transfer1.getTimeStamp(), transfer2.getTimeStamp()));
		Collections.reverse(transfers);

		return transfers;
	}

    /**
     * Gets the completed transfers which have as state cancelled or failed.
     *
     * @return The list the cancelled or failed transfers.
     */
	public ArrayList<AndroidCompletedTransfer> getFailedOrCancelledTransfers(){
		String selectQuery = "SELECT * FROM " + TABLE_COMPLETED_TRANSFERS
				+ " WHERE " + KEY_TRANSFER_STATE + " = '" +  encrypt(MegaTransfer.STATE_CANCELLED +"")
				+ "' OR " + KEY_TRANSFER_STATE + " = '" + encrypt(MegaTransfer.STATE_FAILED +"") + "'"
				+ " ORDER BY " + KEY_TRANSFER_TIMESTAMP + " DESC";
		return getCompletedTransfers(selectQuery);
	}

	/**
	 * Removes the completed transfers which have cancelled or failed as state .
	 */
	public void removeFailedOrCancelledTransfers() {
		String whereCause = KEY_TRANSFER_STATE + " = '" + encrypt(MegaTransfer.STATE_CANCELLED + "")
				+ "' OR " + KEY_TRANSFER_STATE + " = '" + encrypt(MegaTransfer.STATE_FAILED + "") + "'";
		db.delete(TABLE_COMPLETED_TRANSFERS, whereCause, null);
	}

    /**
     * Gets a list with completed transfers depending on the query received by parameter.
     *
     * @param selectQuery   the query which selects specific completed transfers
     * @return The list with the completed transfers.
     */
	public ArrayList<AndroidCompletedTransfer> getCompletedTransfers(String selectQuery) {
		ArrayList<AndroidCompletedTransfer> cTs = new ArrayList<AndroidCompletedTransfer>();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToLast()) {
				do {
					cTs.add(extractAndroidCompletedTransfer(cursor));
				} while (cursor.moveToPrevious());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return cTs;
	}


	public boolean isPasscodeLockEnabled(SQLiteDatabase db){
        logDebug("getPinLockEnabled");
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		String pinLockEnabled = null;
		boolean result = false;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				//get pinLockEnabled
				pinLockEnabled = decrypt(cursor.getString(7));
				if (pinLockEnabled != null && pinLockEnabled.equals("true")) {
					result = true;
				}
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return result;
	}

	/**
	 * Saves attributes in DB.
	 *
	 * @param attr Attributes to save.
	 */
	public void setAttributes (MegaAttributes attr){
		setAttributes(db, attr);
	}

	/**
	 * Saves attributes in DB.
	 *
	 * @param db   DB object to save the attributes.
	 * @param attr Attributes to save.
	 */
	private void setAttributes (SQLiteDatabase db, MegaAttributes attr) {
		if (attr == null) {
			logError("Error: Attributes are null");
			return;
		}

		ContentValues values = new ContentValues();
		values.put(KEY_ATTR_ONLINE, encrypt(attr.getOnline()));
		values.put(KEY_ATTR_INTENTS, encrypt(Integer.toString(attr.getAttemps())));
		values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(attr.getAskSizeDownload()));
		values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(attr.getAskNoAppDownload()));
		values.put(KEY_ACCOUNT_DETAILS_TIMESTAMP, encrypt(attr.getAccountDetailsTimeStamp()));
		values.put(KEY_PAYMENT_METHODS_TIMESTAMP, encrypt(attr.getPaymentMethodsTimeStamp()));
		values.put(KEY_PRICING_TIMESTAMP, encrypt(attr.getPricingTimeStamp()));
		values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP, encrypt(attr.getExtendedAccountDetailsTimeStamp()));
		values.put(KEY_INVALIDATE_SDK_CACHE, encrypt(attr.getInvalidateSdkCache()));
		values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.getUseHttpsOnly()));
		values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.getUseHttpsOnly()));
		values.put(KEY_SHOW_COPYRIGHT, encrypt(attr.getShowCopyright()));
		values.put(KEY_SHOW_NOTIF_OFF, encrypt(attr.getShowNotifOff()));
		values.put(KEY_LAST_PUBLIC_HANDLE, encrypt(Long.toString(attr.getLastPublicHandle())));
		values.put(KEY_LAST_PUBLIC_HANDLE_TIMESTAMP, encrypt(Long.toString(attr.getLastPublicHandleTimeStamp())));
		values.put(KEY_STORAGE_STATE, encrypt(Integer.toString(attr.getStorageState())));
		values.put(KEY_LAST_PUBLIC_HANDLE_TYPE, encrypt(Integer.toString(attr.getLastPublicHandleType())));
		values.put(KEY_MY_CHAT_FILES_FOLDER_HANDLE, encrypt(Long.toString(attr.getMyChatFilesFolderHandle())));
		values.put(KEY_TRANSFER_QUEUE_STATUS, encrypt(attr.getTransferQueueStatus()));
		db.insert(TABLE_ATTRIBUTES, null, values);
	}

	/**
	 * Gets attributes.
	 *
	 * @param db	Current DB.
	 * @return The attributes.
	 */
	private MegaAttributes getAttributes(SQLiteDatabase db) {
		MegaAttributes attr = null;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String online = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_ONLINE)));
				String intents = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_INTENTS)));
				String askSizeDownload = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_ASK_SIZE_DOWNLOAD)));
				String askNoAppDownload = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_ASK_NOAPP_DOWNLOAD)));

				if (!areSDKLogsEnabled() && cursor.getColumnIndex(KEY_FILE_LOGGER_SDK) != INVALID_VALUE) {
					String fileLoggerSDK = decrypt(cursor.getString(getColumnIndex(cursor, KEY_FILE_LOGGER_SDK)));
					updateSDKLogs(Boolean.parseBoolean(fileLoggerSDK));
				}

				String accountDetailsTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ACCOUNT_DETAILS_TIMESTAMP)));
				String paymentMethodsTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PAYMENT_METHODS_TIMESTAMP)));
				String pricingTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_PRICING_TIMESTAMP)));
				String extendedAccountDetailsTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP)));
				String invalidateSdkCache = decrypt(cursor.getString(getColumnIndex(cursor, KEY_INVALIDATE_SDK_CACHE)));

				if (!areKarereLogsEnabled() && cursor.getColumnIndex(KEY_FILE_LOGGER_KARERE) != INVALID_VALUE) {
					String fileLoggerKarere = decrypt(cursor.getString(getColumnIndex(cursor, KEY_FILE_LOGGER_KARERE)));
					updateKarereLogs(Boolean.parseBoolean(fileLoggerKarere));
				}

				String useHttpsOnly = decrypt(cursor.getString(getColumnIndex(cursor, KEY_USE_HTTPS_ONLY)));
				String showCopyright = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_COPYRIGHT)));
				String showNotifOff = decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_NOTIF_OFF)));
				String lastPublicHandle = decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_PUBLIC_HANDLE)));
				String lastPublicHandleTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_PUBLIC_HANDLE_TIMESTAMP)));
				String storageState = decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_STATE)));
				String lastPublicHandleType = decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_PUBLIC_HANDLE_TYPE)));
				String myChatFilesFolderHandle = decrypt(cursor.getString(getColumnIndex(cursor, KEY_MY_CHAT_FILES_FOLDER_HANDLE)));
				String transferQueueStatus = decrypt(cursor.getString(getColumnIndex(cursor, KEY_TRANSFER_QUEUE_STATUS)));

				attr = new MegaAttributes(online,
						intents != null && !intents.isEmpty() ? Integer.parseInt(intents) : 0,
						askSizeDownload, askNoAppDownload, accountDetailsTimeStamp,
						paymentMethodsTimeStamp, pricingTimeStamp, extendedAccountDetailsTimeStamp,
						invalidateSdkCache, useHttpsOnly, showCopyright, showNotifOff,
						lastPublicHandle, lastPublicHandleTimeStamp,
						lastPublicHandleType != null && !lastPublicHandleType.isEmpty() ? Integer.parseInt(lastPublicHandleType) : MegaApiJava.AFFILIATE_TYPE_INVALID,
						storageState != null && !storageState.isEmpty() ? Integer.parseInt(storageState) : MegaApiJava.STORAGE_STATE_UNKNOWN,
						myChatFilesFolderHandle, transferQueueStatus);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}

		return attr;
	}

	/**
	 * Gets attributes.
	 *
	 * @return The attributes.
	 */
	public MegaAttributes getAttributes() {
		return getAttributes(db);
	}

	public int setNonContactFirstName (String name, String handle){
        logDebug("setContactName: " + name + " " + handle);
		ContentValues values = new ContentValues();
		values.put(KEY_NONCONTACT_FIRSTNAME, encrypt(name));
		int rows = db.update(TABLE_NON_CONTACTS, values, KEY_NONCONTACT_HANDLE + " = '" + encrypt(handle) + "'", null);
		if(rows==0){
			values.put(KEY_NONCONTACT_HANDLE, encrypt(handle));
			db.insert(TABLE_NON_CONTACTS, null, values);
		}
		return rows;
	}
	public int setNonContactLastName (String lastName, String handle){

		ContentValues values = new ContentValues();
		values.put(KEY_NONCONTACT_LASTNAME, encrypt(lastName));
		int rows = db.update(TABLE_NON_CONTACTS, values, KEY_NONCONTACT_HANDLE + " = '" + encrypt(handle) + "'", null);
		if(rows==0){
			values.put(KEY_NONCONTACT_HANDLE, encrypt(handle));
			db.insert(TABLE_NON_CONTACTS, null, values);
		}
		return rows;
	}
	public int setNonContactEmail (String email, String handle){

		ContentValues values = new ContentValues();
		values.put(KEY_NONCONTACT_EMAIL, encrypt(email));
		int rows = db.update(TABLE_NON_CONTACTS, values, KEY_NONCONTACT_HANDLE + " = '" + encrypt(handle) + "'", null);
		if(rows==0){
			values.put(KEY_NONCONTACT_HANDLE, encrypt(handle));
			db.insert(TABLE_NON_CONTACTS, null, values);
		}
		return rows;
	}

	public NonContactInfo findNonContactByHandle(String handle){
        logDebug("findNONContactByHandle: " + handle);
		NonContactInfo noncontact = null;

		String selectQuery = "SELECT * FROM " + TABLE_NON_CONTACTS + " WHERE " + KEY_NONCONTACT_HANDLE + " = '" + encrypt(handle)+ "'";
        logDebug("QUERY: " + selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {

			if (cursor != null && cursor.moveToFirst()) {
				int _id = Integer.parseInt(cursor.getString(0));
				String _handle = decrypt(cursor.getString(1));
				String _fullName = decrypt(cursor.getString(2));
				String _firstName = decrypt(cursor.getString(3));
				String _lastName = decrypt(cursor.getString(4));
				String _email = decrypt(cursor.getString(5));

				noncontact = new NonContactInfo(handle, _fullName, _firstName, _lastName, _email);
				return noncontact;
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
	}

	public void setContact (MegaContactDB contact){
        ContentValues values = new ContentValues();
        values.put(KEY_CONTACT_HANDLE, encrypt(contact.getHandle()));
        values.put(KEY_CONTACT_MAIL, encrypt(contact.getMail()));
        values.put(KEY_CONTACT_NAME, encrypt(contact.getName()));
        values.put(KEY_CONTACT_LAST_NAME, encrypt(contact.getLastName()));
        values.put(KEY_CONTACT_NICKNAME, encrypt(contact.getNickname()));
		db.insert(TABLE_CONTACTS, null, values);
	}

	public int setContactName (String name, String mail){
		ContentValues values = new ContentValues();
	    values.put(KEY_CONTACT_NAME, encrypt(name));
	    return db.update(TABLE_CONTACTS, values, KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'", null);
	}

	public int setContactLastName (String lastName, String mail){
		ContentValues values = new ContentValues();
	    values.put(KEY_CONTACT_LAST_NAME, encrypt(lastName));
	    return db.update(TABLE_CONTACTS, values, KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'", null);
	}

	public int setContactNickname(String nickname, long handle) {
		ContentValues values = new ContentValues();
		values.put(KEY_CONTACT_NICKNAME, encrypt(nickname));
		return db.update(TABLE_CONTACTS, values, KEY_CONTACT_HANDLE + " = '" + encrypt(handle + "") + "'", null);
	}

	public int getContactsSize(){
		String selectQuery = "SELECT * FROM " + TABLE_CONTACTS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null) {
				return cursor.getCount();
			} else {
				return 0;
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return 0;
	}

	public int setContactMail (long handle, String mail){
        logDebug("setContactMail: " + handle + " " + mail);

		ContentValues values = new ContentValues();
		values.put(KEY_CONTACT_MAIL, encrypt(mail));
		return db.update(TABLE_CONTACTS, values, KEY_CONTACT_HANDLE + " = '" + encrypt(String.valueOf(handle)) + "'", null);
	}

	public MegaContactDB findContactByHandle(String handle){
        logDebug("findContactByHandle: " + handle);
		MegaContactDB contacts = null;

		String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_CONTACT_HANDLE + " = '" + encrypt(handle) + "'";
        logDebug("QUERY: " + selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int _id = Integer.parseInt(cursor.getString(0));
				String _handle = decrypt(cursor.getString(1));
				String _mail = decrypt(cursor.getString(2));
				String _name = decrypt(cursor.getString(3));
				String _lastName = decrypt(cursor.getString(4));
				String _nickname = decrypt(cursor.getString(5));

				contacts = new MegaContactDB(handle, _mail, _name, _lastName, _nickname);
				return contacts;
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
	}

	public MegaContactDB findContactByEmail(String mail){
        logDebug("findContactByEmail: " + mail);
		MegaContactDB contacts = null;

		String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'";
        logDebug("QUERY: " + selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int _id = Integer.parseInt(cursor.getString(0));
				String _handle = decrypt(cursor.getString(1));
				String _mail = decrypt(cursor.getString(2));
				String _name = decrypt(cursor.getString(3));
				String _lastName = decrypt(cursor.getString(4));
				String _nickname = decrypt(cursor.getString(5));

				contacts = new MegaContactDB(_handle, mail, _name, _lastName, _nickname);

				return contacts;
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
	}

	public long setOfflineFile (MegaOffline offline){
        logDebug("setOfflineFile: " + offline.getHandle());
        ContentValues values = new ContentValues();

        MegaOffline checkInsert = null;
        checkInsert=findByHandle(offline.getHandle());

        if(checkInsert==null){
        	String nullColumnHack = null;

            values.put(KEY_OFF_HANDLE, encrypt(offline.getHandle()));
            values.put(KEY_OFF_PATH, encrypt(offline.getPath()));
            values.put(KEY_OFF_NAME, encrypt(offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getParentId());
            values.put(KEY_OFF_TYPE, encrypt(offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.getHandleIncoming()));

			return db.insert(TABLE_OFFLINE, nullColumnHack, values);
        }
        return -1;
	}

	public long setOfflineFile (MegaOffline offline, SQLiteDatabase db){

        ContentValues values = new ContentValues();

        MegaOffline checkInsert = null;
        checkInsert = findByHandle(offline.getHandle());

        if(checkInsert==null){
        	String nullColumnHack = null;

            values.put(KEY_OFF_HANDLE, encrypt(offline.getHandle()));
            values.put(KEY_OFF_PATH, encrypt(offline.getPath()));
            values.put(KEY_OFF_NAME, encrypt(offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getParentId());
            values.put(KEY_OFF_TYPE, encrypt(offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.getHandleIncoming()));

			return db.insert(TABLE_OFFLINE, nullColumnHack, values);
        }
        return -1;
	}

	public long setOfflineFileOld (MegaOffline offline){

        ContentValues values = new ContentValues();

        MegaOffline checkInsert = null;
        checkInsert = findByHandle(offline.getHandle());

        if(checkInsert==null){
        	String nullColumnHack = null;

            values.put(KEY_OFF_HANDLE, (offline.getHandle()));
            values.put(KEY_OFF_PATH, (offline.getPath()));
            values.put(KEY_OFF_NAME, (offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getParentId());
            values.put(KEY_OFF_TYPE, (offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, (offline.getHandleIncoming()));

			return db.insert(TABLE_OFFLINE, nullColumnHack, values);
        }
        return -1;
	}

	public long setOfflineFileOld (MegaOffline offline, SQLiteDatabase db){

        ContentValues values = new ContentValues();

        MegaOffline checkInsert = null;
        checkInsert = findByHandle(offline.getHandle());

        if(checkInsert==null){
        	String nullColumnHack = null;

            values.put(KEY_OFF_HANDLE, (offline.getHandle()));
            values.put(KEY_OFF_PATH, (offline.getPath()));
            values.put(KEY_OFF_NAME, (offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getParentId());
            values.put(KEY_OFF_TYPE, (offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, (offline.getHandleIncoming()));

			return db.insert(TABLE_OFFLINE, nullColumnHack, values);
        }
        return -1;
	}

	public ArrayList<MegaOffline> getOfflineFiles (){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					int id = Integer.parseInt(cursor.getString(0));
					String handle = decrypt(cursor.getString(1));
					String path = decrypt(cursor.getString(2));
					String name = decrypt(cursor.getString(3));
					int parent = cursor.getInt(4);
					String type = decrypt(cursor.getString(5));
					int incoming = cursor.getInt(6);
					String handleIncoming = decrypt(cursor.getString(7));
					MegaOffline offline = new MegaOffline(id, handle, path, name, parent, type, incoming, handleIncoming);
					listOffline.add(offline);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return listOffline;
	}

	public ArrayList<MegaOffline> getOfflineFilesOld (SQLiteDatabase db){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					int id = Integer.parseInt(cursor.getString(0));
					String handle = (cursor.getString(1));
					String path = (cursor.getString(2));
					String name = (cursor.getString(3));
					int parent = cursor.getInt(4);
					String type = (cursor.getString(5));
					int incoming = cursor.getInt(6);
					String handleIncoming = (cursor.getString(7));
					MegaOffline offline = new MegaOffline(id, handle, path, name, parent, type, incoming, handleIncoming);
					listOffline.add(offline);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return listOffline;
	}

	public boolean exists(long handle){
		//Get the foreign key of the node
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + encrypt(Long.toString(handle)) + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null){
				return cursor.moveToFirst();
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return false;
	}

	public MegaOffline findByHandle(long handle) {
        return findByHandle(String.valueOf(handle));
	}

	public MegaOffline findByHandle(String handle) {
		//Get the foreign key of the node
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '"
				+ encrypt(handle) + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int id = Integer.parseInt(cursor.getString(0));
					String nodeHandle = decrypt(cursor.getString(1));
					String path = decrypt(cursor.getString(2));
					String name = decrypt(cursor.getString(3));
					int parent = cursor.getInt(4);
					String type = decrypt(cursor.getString(5));
					int incoming = cursor.getInt(6);
					String handleIncoming = decrypt(cursor.getString(7));
					return new MegaOffline(id, nodeHandle, path, name, parent, type, incoming,
							handleIncoming);
				}
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
	}

	public ArrayList<MegaOffline> findByParentId(int parentId){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		//Get the foreign key of the node
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PARENT + " = '" + parentId + "'";

		try (Cursor cursor = db.rawQuery(selectQuery, null)) {

			if (cursor != null && cursor.moveToFirst()) {
				do {
					int _id = Integer.parseInt(cursor.getString(0));
					String _handle = decrypt(cursor.getString(1));
					String _path = decrypt(cursor.getString(2));
					String _name = decrypt(cursor.getString(3));
					int _parent = cursor.getInt(4);
					String _type = decrypt(cursor.getString(5));
					int _incoming = cursor.getInt(6);
					String _handleIncoming = decrypt(cursor.getString(7));

					listOffline.add(new MegaOffline(_id, _handle, _path, _name, _parent, _type, _incoming, _handleIncoming));
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return listOffline;
	}

	public MegaOffline findById(int id){

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_ID + " = '" + id + "'";
		MegaOffline mOffline = null;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {

			if (cursor != null && cursor.moveToFirst()) {
				do {
					int _id = Integer.parseInt(cursor.getString(0));
					String _handle = decrypt(cursor.getString(1));
					String _path = decrypt(cursor.getString(2));
					String _name = decrypt(cursor.getString(3));
					int _parent = cursor.getInt(4);
					String _type = decrypt(cursor.getString(5));
					int _incoming = cursor.getInt(6);
					String _handleIncoming = decrypt(cursor.getString(7));
					mOffline = new MegaOffline(_id, _handle, _path, _name, _parent, _type, _incoming, _handleIncoming);

				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return mOffline;
	}

	public int removeById(int id){

		return db.delete(TABLE_OFFLINE, KEY_ID + "="+id, null);

	}

	public ArrayList<MegaOffline> findByPath(String path){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		//Get the foreign key of the node
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + encrypt(path) + "'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					int _id = Integer.parseInt(cursor.getString(0));
					String _handle = decrypt(cursor.getString(1));
					String _path = decrypt(cursor.getString(2));
					String _name = decrypt(cursor.getString(3));
					int _parent = cursor.getInt(4);
					String _type = decrypt(cursor.getString(5));
					int _incoming = cursor.getInt(6);
					String _handleIncoming = decrypt(cursor.getString(7));
					listOffline.add(new MegaOffline(_id, _handle, _path, _name, _parent, _type, _incoming, _handleIncoming));
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return listOffline;
	}

	public MegaOffline findbyPathAndName(String path, String name){

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + encrypt(path) + "'" + "AND " + KEY_OFF_NAME + " = '" + encrypt(name) + "'"  ;

		MegaOffline mOffline = null;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {

			if (cursor != null && cursor.moveToFirst()) {
				do {
					int _id = Integer.parseInt(cursor.getString(0));
					String _handle = decrypt(cursor.getString(1));
					String _path = decrypt(cursor.getString(2));
					String _name = decrypt(cursor.getString(3));
					int _parent = cursor.getInt(4);
					String _type = decrypt(cursor.getString(5));
					int _incoming = cursor.getInt(6);
					String _handleIncoming = decrypt(cursor.getString(7));
					mOffline = new MegaOffline(_id, _handle, _path, _name, _parent, _type, _incoming, _handleIncoming);

				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return mOffline;
	}

	public ArrayList<MegaOffline> getNodesSameParentOffline (String path, String name){

		int _id = -1;
		int _parent = -1;
		String _handle = null;
		String _path = null;
		String _name = null;
		String _type = null;
		int _incoming = 0;
		String _handleIncoming = null;

		//Get the foreign key of the node
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + encrypt(path) + "'" + "AND" + KEY_OFF_NAME + " = '" + encrypt(name) + "'"  ;

		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
//				_id = Integer.parseInt(cursor.getString(0));
//				_handle = decrypt(cursor.getString(1));
//				_path = decrypt(cursor.getString(2));
//				_name = decrypt(cursor.getString(3));
				_parent = cursor.getInt(4);
//				_type = decrypt(cursor.getString(5));
//				_incoming = cursor.getInt(6);
//				_handleIncoming = cursor.getString(7);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();

		//Get the rest of nodes with the same parent (if there be)
		if(_parent!=-1){

			selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PARENT + " = '" + _parent + "'";

			try (Cursor cursor = db.rawQuery(selectQuery, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					do {
						_id = Integer.parseInt(cursor.getString(0));
						_handle = decrypt(cursor.getString(1));
						_path = decrypt(cursor.getString(2));
						_name = decrypt(cursor.getString(3));
						_parent = cursor.getInt(4);
						_type = decrypt(cursor.getString(5));
						_incoming = cursor.getInt(6);
						_handleIncoming = cursor.getString(7);
						MegaOffline offline = new MegaOffline(_handle, _path, _name, _parent, _type, _incoming, _handleIncoming);
						listOffline.add(offline);
					} while (cursor.moveToNext());
				}
			} catch (Exception e) {
				logError("Exception opening or managing DB cursor", e);
			}
		}

		return listOffline;
	}

	public int deleteOfflineFile (MegaOffline mOff) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    return db.delete(TABLE_OFFLINE, KEY_OFF_HANDLE + " = ?",
	            new String[] { encrypt(String.valueOf(mOff.getHandle())) });

	}

	public void setFirstTime (boolean firstTime){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_FIRST_LOGIN + "= '" + encrypt(firstTime + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_FIRST_LOGIN, encrypt(firstTime + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setCamSyncWifi (boolean wifi){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_WIFI + "= '" + encrypt(wifi + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CAM_SYNC_WIFI, encrypt(wifi + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setPreferredViewList (boolean list){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_VIEW_LIST + "= '" + encrypt(list + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PREFERRED_VIEW_LIST, encrypt(list + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setPreferredViewListCamera (boolean list){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_VIEW_LIST_CAMERA + "= '" + encrypt(list + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PREFERRED_VIEW_LIST_CAMERA, encrypt(list + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setPreferredSortCloud (String order){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CLOUD + "= '" + encrypt(order) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PREFERRED_SORT_CLOUD, encrypt(order));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

    public void setPreferredSortCameraUpload(String order) {
        logDebug("set sort camera upload order: " + order);
        setStringValue(TABLE_PREFERENCES, KEY_PREFERRED_SORT_CAMERA_UPLOAD, order);
    }

	public void setPreferredSortOthers (String order){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_OTHERS + "= '" + encrypt(order) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PREFERRED_SORT_OTHERS, encrypt(order));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setLastUploadFolder (String folderPath){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_LAST_UPLOAD_FOLDER + "= '" + encrypt(folderPath + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE UPLOAD FOLDER: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_LAST_UPLOAD_FOLDER, encrypt(folderPath + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setLastCloudFolder (String folderHandle){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_LAST_CLOUD_FOLDER_HANDLE + "= '" + encrypt(folderHandle + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
				logDebug("KEY_LAST_CLOUD_FOLDER_HANDLE UPLOAD FOLDER: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(folderHandle + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setKeepFileNames (boolean charging){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_KEEP_FILE_NAMES + "= '" + encrypt(charging + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC KEEP_FILES: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_KEEP_FILE_NAMES, encrypt(charging + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setCamSyncEnabled (boolean enabled){
        logDebug("setCamSyncEnabled: " + enabled);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_ENABLED + "= '" + encrypt(enabled + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CAM_SYNC_ENABLED, encrypt(enabled + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
			if (enabled) {
				CameraUploadSyncManager.INSTANCE.setPrimaryBackup();
			} else {
				CameraUploadSyncManager.INSTANCE.removePrimaryBackup();
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setSecondaryUploadEnabled (boolean enabled){
        logDebug("setSecondaryUploadEnabled: " + enabled);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_ENABLED + "= '" + encrypt(enabled + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_SEC_FOLDER_ENABLED, encrypt(enabled + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
			// Set or remove corresponding MU backup.
			if (enabled) {
				CameraUploadSyncManager.INSTANCE.setSecondaryBackup();
			} else {
				CameraUploadSyncManager.INSTANCE.removeSecondaryBackup();
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setCamSyncHandle (long handle){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_HANDLE + "= '" + encrypt(handle + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CAM_SYNC_HANDLE, encrypt(handle + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
			logDebug("Set new primary handle: " + handle);
			//Update CU backup when CU target folder changed.
			CameraUploadSyncManager.INSTANCE.updatePrimaryTargetNode(handle);
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setSecondaryFolderHandle (long handle){
        logDebug("setSecondaryFolderHandle: " + handle);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_HANDLE + "= '" + encrypt(handle + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_SEC_FOLDER_HANDLE, encrypt(handle + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
			logDebug("Set new secondary handle: " + handle);
			//Update MU backup when MU target folder changed.
			CameraUploadSyncManager.INSTANCE.updateSecondaryTargetNode(handle);
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setCamSyncLocalPath (String localPath){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_LOCAL_PATH + "= '" + encrypt(localPath + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(localPath + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setUriExternalSDCard (String uriExternalSDCard){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_URI_EXTERNAL_SD_CARD + "= '" + encrypt(uriExternalSDCard) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
				logDebug("KEY_URI_EXTERNAL_SD_CARD URI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_URI_EXTERNAL_SD_CARD, encrypt(uriExternalSDCard));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	/**
	 * Sets the local path selected from an external SD card as Media Uploads local folder.
	 *
	 * @param uriMediaExternalSdCard	local path
	 */
	public void setUriMediaExternalSdCard(String uriMediaExternalSdCard) {
		setStringValue(TABLE_PREFERENCES, KEY_URI_MEDIA_EXTERNAL_SD_CARD, uriMediaExternalSdCard);
	}

	/**
	 * Gets the local path selected in an external SD card as Media Uploads local folder.
	 *
	 * @return The Media Uploads path located in SD card
	 */
	public String getUriMediaExternalSdCard() {
		return getStringValue(TABLE_PREFERENCES, KEY_URI_MEDIA_EXTERNAL_SD_CARD, "");
	}

    public void setSDCardUri (String sdCardUri){
        setStringValue(TABLE_PREFERENCES, KEY_SD_CARD_URI, sdCardUri);
    }

	public void setCameraFolderExternalSDCard (boolean cameraFolderExternalSDCard){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD + "= '" + encrypt(cameraFolderExternalSDCard + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD, encrypt(cameraFolderExternalSDCard + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	/**
	 * Sets the flag to indicate if the local path selected as Media Uploads local folder belongs to an external SD card.
	 *
	 * @param mediaFolderExternalSdCard	true if the local path selected belongs to an external SD card, false otherwise
	 */
	public void setMediaFolderExternalSdCard(boolean mediaFolderExternalSdCard) {
		setStringValue(TABLE_PREFERENCES, KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD, mediaFolderExternalSdCard + "");
	}

	/**
	 * Gets the flag which indicates if the local path selected as Media Uploads local folder belongs to an external SD card.
	 *
	 * @return True if the local path belongs to an external SD card, false otherwise
	 */
	public boolean getMediaFolderExternalSdCard() {
		return getBooleanValue(TABLE_PREFERENCES, KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD, false);
	}

	public void setPasscodeLockType(String passcodeLockType){
        logDebug("setPasscodeLockType");
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();

		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PASSCODE_LOCK_TYPE + "= '" + encrypt(passcodeLockType) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PASSCODE_LOCK_TYPE, encrypt(passcodeLockType));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public String getPasscodeLockType() {
		return getStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_TYPE, "");
	}

	public void setSecondaryFolderPath (String localPath){
        logDebug("setSecondaryFolderPath: " + localPath);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_LOCAL_PATH + "= '" + encrypt(localPath + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(localPath + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setCamSyncFileUpload (int fileUpload){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_FILE_UPLOAD + "= '" + encrypt(fileUpload + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(fileUpload + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setAccountDetailsTimeStamp (){
		setAccountDetailsTimeStamp(System.currentTimeMillis()/1000);
	}

	public void resetAccountDetailsTimeStamp (){
		setAccountDetailsTimeStamp(-1);
	}

	private void setAccountDetailsTimeStamp (long accountDetailsTimeStamp){
        logDebug("setAccountDetailsTimeStamp");

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ACCOUNT_DETAILS_TIMESTAMP + "= '" + encrypt(accountDetailsTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_ACCOUNT_DETAILS_TIMESTAMP, encrypt(accountDetailsTimeStamp + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setPaymentMethodsTimeStamp (){
        logDebug("setPaymentMethodsTimeStamp");
		long paymentMethodsTimeStamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_PAYMENT_METHODS_TIMESTAMP + "= '" + encrypt(paymentMethodsTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PAYMENT_METHODS_TIMESTAMP, encrypt(paymentMethodsTimeStamp + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setPricingTimestamp (){
        logDebug("setPricingTimestamp");
		long creditCardTimestamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_PRICING_TIMESTAMP + "= '" + encrypt(creditCardTimestamp + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PRICING_TIMESTAMP, encrypt(creditCardTimestamp + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setExtendedAccountDetailsTimestamp (){
        logDebug("setExtendedAccountDetailsTimestamp");
		long extendedAccountDetailsTimestamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + "= '" + encrypt(extendedAccountDetailsTimestamp + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP, encrypt(extendedAccountDetailsTimestamp + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void resetExtendedAccountDetailsTimestamp (){
        logDebug("resetExtendedAccountDetailsTimestamp");
		long extendedAccountDetailsTimestamp = -1;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + "= '" + encrypt(extendedAccountDetailsTimestamp + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP, encrypt(extendedAccountDetailsTimestamp + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

    public void setCamSyncTimeStamp(long camSyncTimeStamp) {
        logDebug("setCamSyncTimeStamp: " + camSyncTimeStamp);
        setLongValue(TABLE_PREFERENCES, KEY_CAM_SYNC_TIMESTAMP, camSyncTimeStamp);
    }

    public void setCamVideoSyncTimeStamp(long camVideoSyncTimeStamp) {
        logDebug("setCamVideoSyncTimeStamp: " + camVideoSyncTimeStamp);
        setLongValue(TABLE_PREFERENCES, KEY_CAM_VIDEO_SYNC_TIMESTAMP, camVideoSyncTimeStamp);
    }

    public void setSecSyncTimeStamp(long secSyncTimeStamp) {
        logDebug("setSecSyncTimeStamp: " + secSyncTimeStamp);
        setLongValue(TABLE_PREFERENCES, KEY_SEC_SYNC_TIMESTAMP, secSyncTimeStamp);
    }

    public void setSecVideoSyncTimeStamp (long secVideoSyncTimeStamp){
        logDebug("setSecVideoSyncTimeStamp: " + secVideoSyncTimeStamp);
        setLongValue(TABLE_PREFERENCES,KEY_SEC_VIDEO_SYNC_TIMESTAMP,secVideoSyncTimeStamp);
    }

	/**
	 * Set an integer value into the database.
	 *
	 * @param tableName  Name of the database's table.
	 * @param columnName Name of the table's column.
	 * @param value      Value to set.
	 */
    private void setIntValue(String tableName, String columnName, int value) {
    	setStringValue(tableName, columnName, Integer.toString(value));
	}

	/**
	 * Get an integer value from the database.
	 *
	 * @param tableName    Name of the database's table.
	 * @param columnName   Name of the table's column.
	 * @param defaultValue Default value to return if no result found.
	 * @return Integer value selected from the database.
	 */
	private int getIntValue(String tableName, String columnName, int defaultValue) {
		try {
			String value = getStringValue(tableName, columnName, Integer.toString(defaultValue));
			if (value != null && !value.isEmpty()) {
				return Integer.valueOf(value);
			}
		} catch (Exception e) {
			logWarning("EXCEPTION - Return default value: " + defaultValue, e);
		}

    	return defaultValue;
	}

	/**
	 * Set a long value into the database.
	 *
	 * @param tableName  Name of the database's table.
	 * @param columnName Name of the table's column.
	 * @param value      Value to set.
	 */
    private void setLongValue(String tableName, String columnName, long value) {
		setStringValue(tableName, columnName, Long.toString(value));
    }

	/**
	 * Get a long value from the database.
	 *
	 * @param tableName    Name of the database's table.
	 * @param columnName   Name of the table's column.
	 * @param defaultValue Default value to return if no result found.
	 * @return Long value selected from the database.
	 */
	private long getLongValue(String tableName, String columnName, long defaultValue) {
		try {
			String value = getStringValue(tableName, columnName, Long.toString(defaultValue));
			if (!isTextEmpty(value)) {
				return Long.parseLong(value);
			}
		} catch (Exception e) {
			logWarning("EXCEPTION - Return default value: " + defaultValue, e);
		}

		return defaultValue;
	}

	/**
	 * Set a String value into the database.
	 *
	 * @param tableName  Name of the database's table.
	 * @param columnName Name of the table's column.
	 * @param value      Value to set.
	 */
	private void setStringValue(String tableName, String columnName, String value) {
		if(isTextEmpty(value)) {
			logWarning("Set " + columnName + " with empty value!");
		}

		String selectQuery = "SELECT * FROM " + tableName;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_TABLE = "UPDATE " + tableName + " SET " + columnName + "= '" + encrypt(value) + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_TABLE);
			} else {
				ContentValues values = new ContentValues();
				values.put(columnName, encrypt(value));
				db.insert(tableName, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	/**
	 * Get a String value from the database.
	 *
	 * @param tableName    Name of the database's table.
	 * @param columnName   Name of the table's column.
	 * @param defaultValue Default value to return if no result found.
	 * @return String value selected from the database.
	 */
	private String getStringValue(String tableName, String columnName, String defaultValue) {
		String value = defaultValue;
		String selectQuery = "SELECT " + columnName + " FROM " + tableName + " WHERE " + KEY_ID + " = '1'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				value = decrypt(cursor.getString(0));
				logDebug(columnName + " value: " + value);
			} else {
				logWarning("No value found, setting default");
				ContentValues values = new ContentValues();
				values.put(columnName, encrypt(defaultValue));
				db.insert(tableName, null, values);
				logDebug("Default value: " + defaultValue);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return value;
	}

	/**
	 * Get a boolean value from the database.
	 *
	 * @param tableName    Name of the database's table.
	 * @param columnName   Name of the table's column.
	 * @param defaultValue Default value to return if no result found.
	 * @return Boolean value selected from the database.
	 */
	private boolean getBooleanValue(String tableName, String columnName, boolean defaultValue) {
		try {
			String value = getStringValue(tableName, columnName, Boolean.toString(defaultValue));
			if (value != null && !value.isEmpty()) {
				return Boolean.valueOf(value);
			}
		} catch (Exception e) {
			logWarning("EXCEPTION - Return default value: " + defaultValue, e);
		}

		return defaultValue;
	}

	public void setPasscodeLockEnabled(boolean passcodeLockEnabled){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();

		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PASSCODE_LOCK_ENABLED + "= '" + encrypt(passcodeLockEnabled + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PASSCODE_LOCK_ENABLED, encrypt(passcodeLockEnabled + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public boolean isPasscodeLockEnabled() {
		return getBooleanValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_ENABLED, false);
	}

	public void setPasscodeLockCode(String passcodeLockCode){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();

		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PASSCODE_LOCK_CODE + "= '" + encrypt(passcodeLockCode + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_PASSCODE_LOCK_CODE, encrypt(passcodeLockCode + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public String getPasscodeLockCode() {
		return getStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_CODE, "");
	}

	/**
	 * Sets the time required before ask for the passcode.
	 *
	 * @param requiredTime The time required before ask for the passcode.
	 */
	public void setPasscodeRequiredTime(int requiredTime) {
		setStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_REQUIRE_TIME, requiredTime + "");
	}

	/**
	 * Gets the time required before ask for the passcode.
	 *
	 * @return The time required before ask for the passcode.
	 */
	public int getPasscodeRequiredTime() {
		String string = getStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_REQUIRE_TIME, REQUIRE_PASSCODE_INVALID + "");
		return !isTextEmpty(string) ? Integer.parseInt(string) : REQUIRE_PASSCODE_INVALID;
	}

	/**
	 * Sets if the fingerprint lock setting is enabled or not.
	 *
	 * @param enabled True if the fingerprint is enabled, false otherwise.
	 */
	public void setFingerprintLockEnabled(boolean enabled) {
		setStringValue(TABLE_PREFERENCES, KEY_FINGERPRINT_LOCK, "" + enabled);
	}

	/**
	 * Checks if the fingerprint lock setting is enabled.
	 *
	 * @return True if the fingerprint is enabled, false otherwise.
	 */
	public boolean isFingerprintLockEnabled() {
		return getBooleanValue(TABLE_PREFERENCES, KEY_FINGERPRINT_LOCK, false);
	}

	public void setStorageAskAlways(boolean storageAskAlways) {
		setStringValue(TABLE_PREFERENCES, KEY_STORAGE_ASK_ALWAYS, storageAskAlways + "");
	}

	/**
	 * Sets the flag to indicate if should ask the user about set the current path as default download location.
	 *
	 * @param askSetDownloadLocation true if should ask, false otherwise.
	 */
	public void setAskSetDownloadLocation(boolean askSetDownloadLocation) {
		setStringValue(TABLE_PREFERENCES, KEY_ASK_SET_DOWNLOAD_LOCATION, askSetDownloadLocation + "");
	}

	/**
	 * Gets the flag which indicates if should ask the user about set the current path as default download location.
	 *
	 * @return true if should ask, false otherwise.
	 */
	public boolean getAskSetDownloadLocation() {
		return getBooleanValue(TABLE_PREFERENCES, KEY_ASK_SET_DOWNLOAD_LOCATION, true);
	}

	public void setStorageDownloadLocation (String storageDownloadLocation){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_DOWNLOAD_LOCATION + "= '" + encrypt(storageDownloadLocation + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(storageDownloadLocation + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setAttrAskSizeDownload (String askSizeDownload){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ASK_SIZE_DOWNLOAD + "='" + encrypt(askSizeDownload) + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
				logDebug("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(askSizeDownload));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setAttrAskNoAppDownload (String askNoAppDownload){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ASK_NOAPP_DOWNLOAD + "='" + encrypt(askNoAppDownload) + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
				logDebug("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(askNoAppDownload));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setAttrAttemps (int attemp){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_INTENTS + "='" + encrypt(Integer.toString(attemp) + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
				logDebug("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_ATTR_INTENTS, encrypt(Integer.toString(attemp) + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setUseHttpsOnly (boolean useHttpsOnly){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_USE_HTTPS_ONLY + "='" + encrypt(useHttpsOnly + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
				logDebug("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_USE_HTTPS_ONLY, encrypt(useHttpsOnly + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}


	public String getUseHttpsOnly(){

		String selectQuery = "SELECT " + KEY_USE_HTTPS_ONLY + " FROM " + TABLE_ATTRIBUTES + " WHERE " + KEY_ID + " = '1'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String useHttpsOnly = decrypt(cursor.getString(0));
				return useHttpsOnly;
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return "false";
	}

	public void setShowCopyright (boolean showCopyright){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_SHOW_COPYRIGHT + "='" + encrypt(showCopyright + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_SHOW_COPYRIGHT, encrypt(showCopyright + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}


	public String getShowCopyright (){

		String selectQuery = "SELECT " + KEY_SHOW_COPYRIGHT + " FROM " + TABLE_ATTRIBUTES + " WHERE " + KEY_ID + " = '1'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return decrypt(cursor.getString(0));
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return "true";
	}

	public void setShowNotifOff (boolean showNotifOff){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_SHOW_NOTIF_OFF + "='" + encrypt(showNotifOff + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_SHOW_NOTIF_OFF, encrypt(showNotifOff + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setLastPublicHandle (long handle){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_LAST_PUBLIC_HANDLE + "= '" + encrypt(handle + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_LAST_PUBLIC_HANDLE, encrypt(handle + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void setLastPublicHandleTimeStamp(long lastPublicHandleTimeStamp){
        String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_LAST_PUBLIC_HANDLE_TIMESTAMP + "= '" + encrypt(lastPublicHandleTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
				db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
			} else {
				values.put(KEY_LAST_PUBLIC_HANDLE_TIMESTAMP, encrypt(lastPublicHandleTimeStamp + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

	public void setLastPublicHandleTimeStamp (){
        logDebug("setLastPublicHandleTimeStamp");
		long lastPublicHandleTimeStamp = System.currentTimeMillis()/1000;

		setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp);
	}

	/**
	 * Get the last public handle type value from the database.
	 *
	 * @return Last public handle type value.
	 */
	public int getLastPublicHandleType() {
		logInfo("Getting the last public handle type from DB");
		return getIntValue(TABLE_ATTRIBUTES, KEY_LAST_PUBLIC_HANDLE_TYPE, MegaApiJava.AFFILIATE_TYPE_INVALID);
	}

	/**
	 * Set the last public handle type value into the database.
	 *
	 * @param lastPublicHandleType Last public handle type value.
	 */
	public void setLastPublicHandleType(int lastPublicHandleType) {
		logInfo("Setting the last public handle type in the DB");
		setIntValue(TABLE_ATTRIBUTES, KEY_LAST_PUBLIC_HANDLE_TYPE, lastPublicHandleType);
	}

	/**
	 * Get the storage state value from the database.
	 *
	 * @return Storage state value.
	 */
	public int getStorageState() {
		logInfo("Getting the storage state from DB");
		return getIntValue(TABLE_ATTRIBUTES, KEY_STORAGE_STATE, MegaApiJava.STORAGE_STATE_UNKNOWN);
	}

	/**
	 * Set the storage state value into the database.
	 *
	 * @param storageState Storage state value.
	 */
	public void setStorageState(int storageState) {
		logInfo("Setting the storage state in the DB");
		setIntValue(TABLE_ATTRIBUTES, KEY_STORAGE_STATE, storageState);
	}

	/**
	 * Get the handle of "My chat files" folder from the database.
	 *
	 * @return Handle value.
	 */
	public long getMyChatFilesFolderHandle() {
		logInfo("Getting the storage state from DB");
		return getLongValue(TABLE_ATTRIBUTES, KEY_MY_CHAT_FILES_FOLDER_HANDLE, INVALID_HANDLE);
	}

	/**
	 * Set the handle of "My chat files" folder into the database.
	 *
	 * @param myChatFilesFolderHandle Handle value.
	 */
	public void setMyChatFilesFolderHandle(long myChatFilesFolderHandle) {
		logInfo("Setting the storage state in the DB");
		setLongValue(TABLE_ATTRIBUTES, KEY_MY_CHAT_FILES_FOLDER_HANDLE, myChatFilesFolderHandle);
	}

	/**
	 * Get the status of the transfer queue.
	 *
	 * @return True if the queue is paused, false otherwise.
	 */
	public boolean getTransferQueueStatus() {
		logInfo("Getting the storage state from DB");
		return getBooleanValue(TABLE_ATTRIBUTES, KEY_TRANSFER_QUEUE_STATUS, false);
	}

	/**
	 * Set the status of the transfer queue.
	 *
	 * @param transferQueueStatus True if the queue is paused, false otherwise.
	 */
	public void setTransferQueueStatus(boolean transferQueueStatus) {
		logInfo("Setting the storage state in the DB");
		setStringValue(TABLE_ATTRIBUTES, KEY_TRANSFER_QUEUE_STATUS, transferQueueStatus + "");
	}

	public String getShowNotifOff (){

		String selectQuery = "SELECT " + KEY_SHOW_NOTIF_OFF + " FROM " + TABLE_ATTRIBUTES + " WHERE " + KEY_ID + " = '1'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return decrypt(cursor.getString(0));
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return "true";
	}

	public void setInvalidateSdkCache(boolean invalidateSdkCache){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_INVALIDATE_SDK_CACHE + "='" + encrypt(invalidateSdkCache + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
				logDebug("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_INVALIDATE_SDK_CACHE, encrypt(invalidateSdkCache + ""));
				db.insert(TABLE_ATTRIBUTES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
	}

	public void clearCredentials(){
	    logWarning("Clear local credentials!");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);
        onCreate(db);
	}

	public void clearEphemeral(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EPHEMERAL);
		onCreate(db);
	}

	public void clearPreferences(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);
        onCreate(db);
	}

	public void clearAttributes(){
        long lastPublicHandle;
        long lastPublicHandleTimeStamp = -1;
        int lastPublicHandleType = MegaApiJava.AFFILIATE_TYPE_INVALID;
        try {
            MegaAttributes attributes = getAttributes();
            lastPublicHandle = attributes.getLastPublicHandle();
            lastPublicHandleTimeStamp = attributes.getLastPublicHandleTimeStamp();
            lastPublicHandleType = attributes.getLastPublicHandleType();
		} catch (Exception e) {
			logWarning("EXCEPTION getting last public handle info.", e);
			lastPublicHandle = INVALID_HANDLE;
		}
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRIBUTES);
		onCreate(db);
		if (lastPublicHandle != INVALID_HANDLE) {
		    try{
		        setLastPublicHandle(lastPublicHandle);
		        setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp);
				setLastPublicHandleType(lastPublicHandleType);
			} catch (Exception e) {
				logWarning("EXCEPTION saving last public handle info.", e);
			}
        }
	}

	public void clearContacts(){
		db.execSQL("DELETE FROM " + TABLE_CONTACTS);
	}

	public void clearNonContacts(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NON_CONTACTS);
		onCreate(db);
	}

	public void clearChatItems(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_ITEMS);
		onCreate(db);
	}

	public void clearChatSettings(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_SETTINGS);
		onCreate(db);
	}

	public void clearOffline(SQLiteDatabase db){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE);
		onCreate(db);
	}

	public void clearOffline(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE);
		onCreate(db);
	}

    public void clearCompletedTransfers(){
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMPLETED_TRANSFERS);
        onCreate(db);
    }

	public void clearPendingMessage(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING_MSG);
		onCreate(db);
	}

	/**
	 * Adds a pending message.
	 *
	 * @param message Pending message to add.
	 * @return The identifier of the pending message.
	 */
	public long addPendingMessage(PendingMessageSingle message) {
		return addPendingMessage(message, PendingMessageSingle.STATE_PREPARING);
	}

	/**
	 * Adds a pending message from File Explorer.
	 *
	 * @param message Pending message to add.
	 * @return The identifier of the pending message.
	 */
	public long addPendingMessageFromExplorer(PendingMessageSingle message) {
		return addPendingMessage(message, PendingMessageSingle.STATE_PREPARING_FROM_EXPLORER);
	}

	/**
	 * Adds a pending message.
	 *
	 * @param message Pending message to add.
	 * @param state	  State of the pending message.
	 * @return The identifier of the pending message.
	 */
	public long addPendingMessage(PendingMessageSingle message, int state) {
		ContentValues values = new ContentValues();
		values.put(KEY_PENDING_MSG_ID_CHAT, encrypt(message.getChatId() + ""));
		values.put(KEY_PENDING_MSG_TIMESTAMP, encrypt(message.getUploadTimestamp() + ""));
		values.put(KEY_PENDING_MSG_FILE_PATH, encrypt(message.getFilePath()));
		values.put(KEY_PENDING_MSG_FINGERPRINT, encrypt(message.getFingerprint()));
		values.put(KEY_PENDING_MSG_NAME, encrypt(message.getName()));
		values.put(KEY_PENDING_MSG_TRANSFER_TAG, INVALID_ID);
		values.put(KEY_PENDING_MSG_STATE, state);

		return db.insert(TABLE_PENDING_MSG_SINGLE, null, values);
	}

	public PendingMessageSingle findPendingMessageById(long messageId){
        logDebug("findPendingMessageById");
//		String id = messageId+"";
		PendingMessageSingle pendMsg = null;
		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG_SINGLE + " WHERE " +KEY_ID + " ='"+ messageId+"'";
        logDebug("QUERY: " + selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {

			if (cursor != null && cursor.moveToFirst()) {
//				long id = Integer.parseInt(cursor.getString(0));
				long chatId = Long.parseLong(decrypt(cursor.getString(1)));
				long timestamp = Long.parseLong(decrypt(cursor.getString(2)));
				String idKarereString = decrypt(cursor.getString(3));
				long idTempKarere = -1;
				if (idKarereString != null && (!idKarereString.isEmpty())) {
					idTempKarere = Long.parseLong(idKarereString);
				}
				String filePath = decrypt(cursor.getString(4));
				String name = decrypt(cursor.getString(5));

				String nodeHandleString = decrypt(cursor.getString(6));
				long nodeHandle = -1;
				if (nodeHandleString != null && (!nodeHandleString.isEmpty())) {
					nodeHandle = Long.parseLong(nodeHandleString);
				}

				String fingerPrint = decrypt(cursor.getString(7));
				int transferTag = cursor.getInt(8);
				int state = cursor.getInt(9);

				pendMsg = new PendingMessageSingle(messageId, chatId, timestamp, idTempKarere, filePath, fingerPrint, name, nodeHandle, transferTag, state);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return pendMsg;
	}

	/**
	 * Updates a pending message.
	 *
	 * @param idMessage   Identifier of the pending message.
	 * @param transferTag Identifier of the transfer.
	 */
	public void updatePendingMessageOnTransferStart(long idMessage, int transferTag) {
		updatePendingMessage(idMessage, transferTag, INVALID_OPTION, PendingMessageSingle.STATE_UPLOADING);
	}

	/**
	 * Updates a pending message.
	 *
	 * @param idMessage  Identifier of the pending message.
	 * @param nodeHandle Handle of the node already uploaded.
	 * @param state      State of the pending message.
	 */
	public void updatePendingMessageOnTransferFinish(long idMessage, String nodeHandle, int state) {
		updatePendingMessage(idMessage, INVALID_ID, nodeHandle, state);
	}

	/**
	 * Updates a pending message.
	 *
	 * @param idMessage   Identifier of the pending message.
	 * @param transferTag Identifier of the transfer.
	 * @param nodeHandle  Handle of the node already uploaded.
	 * @param state       State of the pending message.
	 */
	public void updatePendingMessage(long idMessage, int transferTag, String nodeHandle, int state) {
		ContentValues values = new ContentValues();

		if (transferTag != INVALID_ID) {
			values.put(KEY_PENDING_MSG_TRANSFER_TAG, transferTag);
		}

		values.put(KEY_PENDING_MSG_NODE_HANDLE, encrypt(nodeHandle));
		values.put(KEY_PENDING_MSG_STATE, state);
		String where = KEY_ID + "=" + idMessage;

		db.update(TABLE_PENDING_MSG_SINGLE, values, where, null);
	}

	public void updatePendingMessageOnAttach(long idMessage, String temporalId, int state) {

		ContentValues values = new ContentValues();
        logDebug("ID of my pending message to update: " + temporalId);
		values.put(KEY_PENDING_MSG_TEMP_KARERE, encrypt(temporalId));
		values.put(KEY_PENDING_MSG_STATE, state);
		String where = KEY_ID + "=" +idMessage;

		int rows = db.update(TABLE_PENDING_MSG_SINGLE, values, where, null);
        logDebug("Rows updated: " + rows);
	}

	public ArrayList<AndroidMegaChatMessage> findPendingMessagesNotSent(long idChat) {
        logDebug("findPendingMessagesNotSent");
		ArrayList<AndroidMegaChatMessage> pendMsgs = new ArrayList<>();
		String chat = idChat + "";

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG_SINGLE + " WHERE " + KEY_PENDING_MSG_STATE + " < " + PendingMessageSingle.STATE_SENT + " AND " + KEY_ID_CHAT + " ='" + encrypt(chat) + "'";
        logDebug("QUERY: " + selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(0);
					long chatId = Long.parseLong(decrypt(cursor.getString(1)));
					long timestamp = Long.parseLong(decrypt(cursor.getString(2)));
					String idKarereString = decrypt(cursor.getString(3));
					long idTempKarere = -1;
					if (idKarereString != null && (!idKarereString.isEmpty())) {
						idTempKarere = Long.parseLong(idKarereString);
					}
					String filePath = decrypt(cursor.getString(4));
					String name = decrypt(cursor.getString(5));

					String nodeHandleString = decrypt(cursor.getString(6));
					long nodeHandle = -1;
					if (nodeHandleString != null && (!nodeHandleString.isEmpty())) {
						nodeHandle = Long.parseLong(nodeHandleString);
					}

					String fingerPrint = decrypt(cursor.getString(7));
					int transferTag = cursor.getInt(8);
					int state = cursor.getInt(9);

					PendingMessageSingle pendMsg = new PendingMessageSingle(id, chatId, timestamp, idTempKarere, filePath, fingerPrint, name, nodeHandle, transferTag, state);

					AndroidMegaChatMessage aPMsg = new AndroidMegaChatMessage(pendMsg, true);
					pendMsgs.add(aPMsg);

				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        logDebug("Found: " + pendMsgs.size());
		return pendMsgs;
	}

	public long findPendingMessageByIdTempKarere(long idTemp){
        logDebug("findPendingMessageById: " + idTemp);
		String idPend = idTemp+"";
		long id = -1;

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG_SINGLE + " WHERE " + KEY_PENDING_MSG_TEMP_KARERE + " = '" + encrypt(idPend) + "'";
		logDebug("QUERY: "+selectQuery);
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				id = cursor.getLong(0);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return id;
	}

	public void removeSentPendingMessages(){
		logDebug("removeSentPendingMessages");
		int rows = db.delete(TABLE_PENDING_MSG_SINGLE, KEY_PENDING_MSG_STATE + "="+PendingMessageSingle.STATE_SENT, null);
	}

	public void removePendingMessageByChatId(long idChat){
		logDebug("removePendingMessageByChatId");
		int rows = db.delete(TABLE_PENDING_MSG_SINGLE, KEY_PENDING_MSG_ID_CHAT + " = '"+ encrypt(idChat + "") +"'", null);
	}

	public void removePendingMessageById(long idMsg){
		int rows = db.delete(TABLE_PENDING_MSG_SINGLE, KEY_ID + "="+idMsg, null);
	}

    public String getAutoPlayEnabled(){

        String selectQuery = "SELECT " + KEY_AUTO_PLAY + " FROM " + TABLE_PREFERENCES + " WHERE " + KEY_ID + " = '1'";
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return decrypt(cursor.getString(0));
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
        return "false";
    }

    public String getSDCardUri(){
        return getStringValue(TABLE_PREFERENCES, KEY_SD_CARD_URI, "");
    }

    public void setAutoPlayEnabled(String enabled){
		logDebug("setAutoPlayEnabled");

        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_AUTO_PLAY + "='" + encrypt(enabled + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_AUTO_PLAY, encrypt(enabled + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

    public void setShowInviteBanner(String show){
        logDebug("setCloseInviteBanner");

        String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SHOW_INVITE_BANNER + "='" + encrypt(show + "") + "' WHERE " + KEY_ID + " ='1'";
				db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			} else {
				values.put(KEY_SHOW_INVITE_BANNER, encrypt(show + ""));
				db.insert(TABLE_PREFERENCES, null, values);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
    }

	public ArrayList<SDTransfer> getSDTransfers() {
		ArrayList<SDTransfer> sdTransfers = new ArrayList<>();
		String selectQuery = "SELECT * FROM " + TABLE_SD_TRANSFERS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToLast()) {
				do {
					int tag = Integer.parseInt(cursor.getString(1));
					String name = decrypt(cursor.getString(2));
					String size = decrypt(cursor.getString(3));
					String nodeHandle = decrypt(cursor.getString(4));
					String path = decrypt(cursor.getString(5));
					String appData = decrypt(cursor.getString(6));

					sdTransfers.add(new SDTransfer(tag, name, size, nodeHandle, path, appData));
				} while (cursor.moveToPrevious());
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return sdTransfers;
	}

	public long addSDTransfer(SDTransfer transfer) {
		ContentValues values = new ContentValues();
		values.put(KEY_SD_TRANSFERS_TAG, transfer.getTag());
		values.put(KEY_SD_TRANSFERS_NAME, encrypt(transfer.getName()));
		values.put(KEY_SD_TRANSFERS_SIZE, encrypt(transfer.getSize()));
		values.put(KEY_SD_TRANSFERS_HANDLE, encrypt(transfer.getNodeHandle()));
		values.put(KEY_SD_TRANSFERS_PATH, encrypt(transfer.getPath()));
		values.put(KEY_SD_TRANSFERS_APP_DATA, encrypt(transfer.getAppData()));

		return db.insert(TABLE_SD_TRANSFERS, null, values);
	}

	public void removeSDTransfer(int tag) {
		db.delete(TABLE_SD_TRANSFERS,
				KEY_SD_TRANSFERS_TAG + "=" + tag,
				null);
	}

    public boolean saveBackup(Backup backup) {
        ContentValues values = new ContentValues();
        values.put(KEY_BACKUP_ID, encrypt(Long.toString(backup.getBackupId())));
        values.put(KEY_BACKUP_TYPE, backup.getBackupType());
        values.put(KEY_BACKUP_TARGET_NODE, encrypt(Long.toString(backup.getTargetNode())));
        values.put(KEY_BACKUP_LOCAL_FOLDER, encrypt(backup.getLocalFolder()));
        values.put(KEY_BACKUP_NAME, encrypt(backup.getBackupName()));
        values.put(KEY_BACKUP_STATE,backup.getState());
        values.put(KEY_BACKUP_SUB_STATE, backup.getSubState());
        values.put(KEY_BACKUP_EXTRA_DATA, encrypt(backup.getExtraData()));
        values.put(KEY_BACKUP_START_TIME, encrypt(Long.toString(backup.getStartTimestamp())));
        values.put(KEY_BACKUP_LAST_TIME, encrypt(Long.toString(backup.getLastFinishTimestamp())));
        values.put(KEY_BACKUP_TARGET_NODE_PATH, encrypt(backup.getTargetFolderPath()));
        values.put(KEY_BACKUP_EX, encrypt(Boolean.toString(backup.isExcludeSubFolders())));
        values.put(KEY_BACKUP_DEL, encrypt(Boolean.toString(backup.isDeleteEmptySubFolders())));
        // Default value is false.
        values.put(KEY_BACKUP_OUTDATED, encrypt(Boolean.toString(false)));
        long result = db.insertOrThrow(TABLE_BACKUPS, null, values);
        if(result != -1) {
            logDebug("Save sync pair " + backup + " successfully, row id is: " + result);
            return true;
        } else {
            logError("Save sync pair " + backup + " failed");
            return false;
        }
    }

    public Backup getCuBackup() {
        return getBackupByType(BACKUP_TYPE_CAMERA_UPLOADS);
    }

    public Backup getMuBackup() {
        return getBackupByType(BACKUP_TYPE_MEDIA_UPLOADS);
    }

    private Backup getBackupByType(int type) {
        String selectQuery = "SELECT * FROM " + TABLE_BACKUPS + " WHERE " + KEY_BACKUP_TYPE + " = " + type +
                " AND " + KEY_BACKUP_OUTDATED + " = '" + encrypt(Boolean.FALSE.toString()) + "' ORDER BY " + KEY_ID + " DESC";
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return getBackupFromCursor(cursor);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
    }

    public void setBackupAsOutdated(long id) {
        Backup backup = getBackupById(id);
        if(backup != null) {
            backup.setOutdated(true);
            updateBackup(backup);
        }
    }

    public Backup getBackupById(long id) {
        String selectQuery = "SELECT * FROM " + TABLE_BACKUPS + " WHERE " + KEY_BACKUP_ID + " = '" + encrypt(Long.toString(id)) + "'";
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return getBackupFromCursor(cursor);
			}
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
    }

    public List<Backup> getAllBackups() {
        String selectQuery = "SELECT * FROM " + TABLE_BACKUPS;
		try (Cursor cursor = db.rawQuery(selectQuery, null)) {
			List<Backup> list = new ArrayList<>();
			if (cursor != null) {
				while (cursor.moveToNext()) {
					list.add(getBackupFromCursor(cursor));
				}
			}
			return list;
		} catch (Exception e) {
			logError("Exception opening or managing DB cursor", e);
		}
		return null;
    }

    private Backup getBackupFromCursor(Cursor cursor) {
        return new Backup(
                Long.parseLong(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_ID)))),
                cursor.getInt(cursor.getColumnIndex(KEY_BACKUP_TYPE)),
                Long.parseLong(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_TARGET_NODE)))),
                decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_LOCAL_FOLDER))),
                decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_NAME))),
                cursor.getInt(cursor.getColumnIndex(KEY_BACKUP_STATE)),
                cursor.getInt(cursor.getColumnIndex(KEY_BACKUP_SUB_STATE)),
                decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_EXTRA_DATA))),
                Long.parseLong(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_START_TIME)))),
                Long.parseLong(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_LAST_TIME)))),
                decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_TARGET_NODE_PATH))),
                Boolean.parseBoolean(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_EX)))),
                Boolean.parseBoolean(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_DEL)))),
                Boolean.parseBoolean(decrypt(cursor.getString(cursor.getColumnIndex(KEY_BACKUP_OUTDATED))))
        );
    }

    public void deleteBackupById(long id) {
        db.execSQL(BackupToolsKt.deleteSQL(id));
    }

    public void updateBackup(Backup backup) {
        db.execSQL(BackupToolsKt.updateSQL(backup));
    }

    public void clearBackups() {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BACKUPS);
        onCreate(db);
    }

    /**
     * Get the index of a column in a cursor.
     * Avoid to access column with hardcode index.
     *
     * @param cursor Cursor object which has the column.
     * @param columnName Name of the column.
     * @return The index of the column in the cursor.
     */
    private int getColumnIndex(Cursor cursor, String columnName) {
        return cursor.getColumnIndex(columnName);
    }
}
