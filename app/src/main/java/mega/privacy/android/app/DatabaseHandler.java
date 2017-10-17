package mega.privacy.android.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;

import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.NonContactInfo;
import mega.privacy.android.app.lollipop.megachat.PendingMessage;
import mega.privacy.android.app.lollipop.megachat.PendingNodeAttachment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;


public class DatabaseHandler extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 35;
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
    private static final String KEY_CAM_SYNC_CHARGING = "camSyncCharging";
    private static final String KEY_KEEP_FILE_NAMES = "keepFileNames";
    private static final String KEY_PIN_LOCK_ENABLED = "pinlockenabled";
    private static final String KEY_PIN_LOCK_TYPE = "pinlocktype";
    private static final String KEY_PIN_LOCK_CODE = "pinlockcode";
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
    private static final String KEY_STORAGE_ADVANCED_DEVICES = "storageadvanceddevices";
    private static final String KEY_PREFERRED_VIEW_LIST = "preferredviewlist";
    private static final String KEY_PREFERRED_VIEW_LIST_CAMERA = "preferredviewlistcamera";
    private static final String KEY_URI_EXTERNAL_SD_CARD = "uriexternalsdcard";
    private static final String KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD = "camerafolderexternalsdcard";
    private static final String KEY_CONTACT_HANDLE = "handle";
    private static final String KEY_CONTACT_MAIL = "mail";
    private static final String KEY_CONTACT_NAME = "name";
    private static final String KEY_CONTACT_LAST_NAME = "lastname";
	private static final String KEY_PREFERRED_SORT_CLOUD = "preferredsortcloud";
	private static final String KEY_PREFERRED_SORT_CONTACTS = "preferredsortcontacts";
	private static final String KEY_PREFERRED_SORT_OTHERS = "preferredsortothers";
	private static final String KEY_FILE_LOGGER_SDK = "filelogger";
	private static final String KEY_FILE_LOGGER_KARERE = "fileloggerkarere";
	private static final String KEY_USE_HTTPS_ONLY = "usehttpsonly";

	private static final String KEY_ACCOUNT_DETAILS_TIMESTAMP = "accountdetailstimestamp";
	private static final String KEY_PAYMENT_METHODS_TIMESTAMP = "paymentmethodsstimestamp";
	private static final String KEY_PRICING_TIMESTAMP = "pricingtimestamp";
	private static final String KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP = "extendedaccountdetailstimestamp";

	private static final String KEY_CHAT_HANDLE = "chathandle";
	private static final String KEY_CHAT_ITEM_NOTIFICATIONS = "chatitemnotifications";
	private static final String KEY_CHAT_ITEM_RINGTONE = "chatitemringtone";
	private static final String KEY_CHAT_ITEM_SOUND_NOTIFICATIONS = "chatitemnotificationsound";

	private static final String KEY_NONCONTACT_HANDLE = "noncontacthandle";
	private static final String KEY_NONCONTACT_FULLNAME = "noncontactfullname";
	private static final String KEY_NONCONTACT_FIRSTNAME = "noncontactfirstname";
	private static final String KEY_NONCONTACT_LASTNAME = "noncontactlastname";
	private static final String KEY_NONCONTACT_EMAIL = "noncontactemail";

	private static final String KEY_CHAT_ENABLED = "chatenabled";
	private static final String KEY_CHAT_NOTIFICATIONS_ENABLED = "chatnotifications";
	private static final String KEY_CHAT_SOUND_NOTIFICATIONS = "chatnotificationsound";
	private static final String KEY_CHAT_VIBRATION_ENABLED = "chatvibrationenabled";
	private static final String KEY_CHAT_STATUS = "chatstatus";

	private static final String KEY_INVALIDATE_SDK_CACHE = "invalidatesdkcache";

	private static final String KEY_TRANSFER_FILENAME = "transferfilename";
	private static final String KEY_TRANSFER_TYPE = "transfertype";
	private static final String KEY_TRANSFER_STATE = "transferstate";
	private static final String KEY_TRANSFER_SIZE = "transfersize";
	private static final String KEY_TRANSFER_HANDLE = "transferhandle";

	private static final String KEY_FIRST_LOGIN_CHAT = "firstloginchat";

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


    private static DatabaseHandler instance;
    
    private static SQLiteDatabase db;

    public static synchronized DatabaseHandler getDbHandler(Context context){
    	
    	log("getDbHandler");
    	
    	if (instance == null){
    		log("INSTANCE IS NULL");
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
		
		log("onCreate");
        String CREATE_OFFLINE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_OFFLINE + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_OFF_HANDLE + " TEXT," + KEY_OFF_PATH + " TEXT," + KEY_OFF_NAME + " TEXT," + 
        		KEY_OFF_PARENT + " INTEGER," + KEY_OFF_TYPE + " INTEGER, " + KEY_OFF_INCOMING + " INTEGER, " + KEY_OFF_HANDLE_INCOMING + " INTEGER "+")";
        db.execSQL(CREATE_OFFLINE_TABLE);
		
		String CREATE_CREDENTIALS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CREDENTIALS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_EMAIL + " TEXT, " 
                + KEY_SESSION + " TEXT, " + KEY_FIRST_NAME + " TEXT, " +  KEY_LAST_NAME + " TEXT, " + KEY_MY_HANDLE + " TEXT" + ")";
        db.execSQL(CREATE_CREDENTIALS_TABLE);
        
        String CREATE_PREFERENCES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PREFERENCES + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_FIRST_LOGIN + " BOOLEAN, "
        		+ KEY_CAM_SYNC_ENABLED + " BOOLEAN, " + KEY_CAM_SYNC_HANDLE + " TEXT, "
        		+ KEY_CAM_SYNC_LOCAL_PATH + " TEXT, " + KEY_CAM_SYNC_WIFI + " BOOLEAN, " 
        		+ KEY_CAM_SYNC_FILE_UPLOAD + " TEXT, " + KEY_PIN_LOCK_ENABLED + " TEXT, " + 
        		KEY_PIN_LOCK_CODE + " TEXT, " + KEY_STORAGE_ASK_ALWAYS + " TEXT, " +
        		KEY_STORAGE_DOWNLOAD_LOCATION + " TEXT, " + KEY_CAM_SYNC_TIMESTAMP + " TEXT, " + 
        		KEY_CAM_SYNC_CHARGING + " BOOLEAN, " + KEY_LAST_UPLOAD_FOLDER + " TEXT, "+
        		KEY_LAST_CLOUD_FOLDER_HANDLE + " TEXT, " + KEY_SEC_FOLDER_ENABLED + " TEXT, " + KEY_SEC_FOLDER_LOCAL_PATH + 
        		" TEXT, "+ KEY_SEC_FOLDER_HANDLE + " TEXT, " + KEY_SEC_SYNC_TIMESTAMP+" TEXT, "+KEY_KEEP_FILE_NAMES + " BOOLEAN, "+
        		KEY_STORAGE_ADVANCED_DEVICES+ "	BOOLEAN, "+ KEY_PREFERRED_VIEW_LIST+ "	BOOLEAN, "+KEY_PREFERRED_VIEW_LIST_CAMERA+ " BOOLEAN, " +
        		KEY_URI_EXTERNAL_SD_CARD + " TEXT, " + KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD + " BOOLEAN, " + KEY_PIN_LOCK_TYPE + " TEXT, " +
				KEY_PREFERRED_SORT_CLOUD + " TEXT, " + KEY_PREFERRED_SORT_CONTACTS + " TEXT, " +KEY_PREFERRED_SORT_OTHERS + " TEXT," +
				KEY_FIRST_LOGIN_CHAT + " BOOLEAN" +")";
        
        db.execSQL(CREATE_PREFERENCES_TABLE);
        
        String CREATE_ATTRIBUTES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTRIBUTES + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_ATTR_ONLINE + " TEXT, " + KEY_ATTR_INTENTS + " TEXT, " + 
        		KEY_ATTR_ASK_SIZE_DOWNLOAD+ "	BOOLEAN, "+KEY_ATTR_ASK_NOAPP_DOWNLOAD+ " BOOLEAN, " + KEY_FILE_LOGGER_SDK +" TEXT, " + KEY_ACCOUNT_DETAILS_TIMESTAMP +" TEXT, " +
				KEY_PAYMENT_METHODS_TIMESTAMP +" TEXT, " + KEY_PRICING_TIMESTAMP +" TEXT, " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP +" TEXT, " + KEY_INVALIDATE_SDK_CACHE + " TEXT, " + KEY_FILE_LOGGER_KARERE +" TEXT, " + KEY_USE_HTTPS_ONLY +" TEXT" + ")";
        db.execSQL(CREATE_ATTRIBUTES_TABLE);

        String CREATE_CONTACTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CONTACT_HANDLE + " TEXT, " + KEY_CONTACT_MAIL + " TEXT, " + 
        		KEY_CONTACT_NAME+ " TEXT, "+KEY_CONTACT_LAST_NAME+ " TEXT"+")";
        db.execSQL(CREATE_CONTACTS_TABLE);

		String CREATE_CHAT_ITEM_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_ITEMS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHAT_HANDLE + " TEXT, " + KEY_CHAT_ITEM_NOTIFICATIONS + " BOOLEAN, " +
				KEY_CHAT_ITEM_RINGTONE+ " TEXT, "+KEY_CHAT_ITEM_SOUND_NOTIFICATIONS+ " TEXT"+")";
		db.execSQL(CREATE_CHAT_ITEM_TABLE);

		String CREATE_NONCONTACT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NON_CONTACTS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_NONCONTACT_HANDLE + " TEXT, " + KEY_NONCONTACT_FULLNAME + " TEXT, " +
				KEY_NONCONTACT_FIRSTNAME+ " TEXT, "+KEY_NONCONTACT_LASTNAME+ " TEXT, "+ KEY_NONCONTACT_EMAIL + " TEXT"+")";
		db.execSQL(CREATE_NONCONTACT_TABLE);

		String CREATE_CHAT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CHAT_SETTINGS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHAT_ENABLED + " BOOLEAN, " + KEY_CHAT_NOTIFICATIONS_ENABLED + " BOOLEAN, " +
				KEY_CHAT_SOUND_NOTIFICATIONS+ " TEXT, "+KEY_CHAT_VIBRATION_ENABLED+ " BOOLEAN, "+ KEY_CHAT_STATUS + " TEXT"+")";
		db.execSQL(CREATE_CHAT_TABLE);

		String CREATE_COMPLETED_TRANSFER_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_COMPLETED_TRANSFERS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_TRANSFER_FILENAME + " TEXT, " + KEY_TRANSFER_TYPE + " TEXT, " +
				KEY_TRANSFER_STATE+ " TEXT, "+ KEY_TRANSFER_SIZE+ " TEXT, " + KEY_TRANSFER_HANDLE + " TEXT"+")";
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
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		log("onUpgrade");
		
//		UserCredentials userCredentials = null;
//		
//		String selectQueryCredentials = "SELECT  * FROM " + TABLE_CREDENTIALS;
//		Cursor cursorCredentials = db.rawQuery(selectQueryCredentials, null);		
//		if (cursorCredentials.moveToFirst()) {
//			int id = Integer.parseInt(cursorCredentials.getString(0));
//			String email = decrypt(cursorCredentials.getString(1));
//			String session = decrypt(cursorCredentials.getString(2));
//			userCredentials = new UserCredentials(email, session);
//		}
//		cursorCredentials.close();
//		
//		MegaPreferences prefs = null;
//		String selectQueryPref = "SELECT * FROM " + TABLE_PREFERENCES;
//		Cursor cursorPref = db.rawQuery(selectQueryPref, null);
//		if (cursorPref.moveToFirst()){
//			int id = Integer.parseInt(cursorPref.getString(0));
//			String firstTime = decrypt(cursorPref.getString(1));
//			String camSyncEnabled = decrypt(cursorPref.getString(2));
//			String camSyncHandle = decrypt(cursorPref.getString(3));
//			String camSyncLocalPath = decrypt(cursorPref.getString(4));
//			String wifi = decrypt(cursorPref.getString(5));
//			String fileUpload = decrypt(cursorPref.getString(6));
//			String pinLockEnabled = decrypt(cursorPref.getString(7));
//			String pinLockCode = decrypt(cursorPref.getString(8));
//			String askAlways = decrypt(cursorPref.getString(9));
//			String downloadLocation = decrypt(cursorPref.getString(10));
//			String camSyncTimeStamp = decrypt(cursorPref.getString(11));
//			prefs = new MegaPreferences(firstTime, wifi, camSyncEnabled, camSyncHandle, camSyncLocalPath, fileUpload, camSyncTimeStamp, pinLockEnabled, pinLockCode, askAlways, downloadLocation);
//		}
//		cursorPref.close();
//        
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES); 
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRIBUTES);
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE);
//        onCreate(db);
//        
//        ContentValues valuesCredentials = new ContentValues();
//        valuesCredentials.put(KEY_EMAIL, encrypt(userCredentials.getEmail()));
//        valuesCredentials.put(KEY_SESSION, encrypt(userCredentials.getSession()));
//        db.insert(TABLE_CREDENTIALS, null, valuesCredentials);
//        
//        ContentValues valuesPref = new ContentValues();
//        valuesPref.put(KEY_FIRST_LOGIN, encrypt(prefs.getFirstTime()));
//        valuesPref.put(KEY_CAM_SYNC_WIFI, encrypt(prefs.getCamSyncWifi()));
//        valuesPref.put(KEY_CAM_SYNC_ENABLED, encrypt(prefs.getCamSyncEnabled()));
//        valuesPref.put(KEY_CAM_SYNC_HANDLE, encrypt(prefs.getCamSyncHandle()));
//        valuesPref.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(prefs.getCamSyncLocalPath()));
//        valuesPref.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(prefs.getCamSyncFileUpload()));
//        valuesPref.put(KEY_PIN_LOCK_ENABLED, encrypt(prefs.getPinLockEnabled()));
//        valuesPref.put(KEY_PIN_LOCK_CODE, encrypt(prefs.getPinLockCode()));
//        valuesPref.put(KEY_STORAGE_ASK_ALWAYS, encrypt(prefs.getStorageAskAlways()));
//        valuesPref.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(prefs.getStorageDownloadLocation()));
//        valuesPref.put(KEY_CAM_SYNC_TIMESTAMP, encrypt(prefs.getCamSyncTimeStamp()));
//        valuesPref.put(KEY_CAM_SYNC_CHARGING, encrypt("false"));
//        db.insert(TABLE_PREFERENCES, null, valuesPref);
		
		if (oldVersion <= 7){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_CAM_SYNC_CHARGING + " BOOLEAN;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_CHARGING + " = '" + encrypt("false") + "';");
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
			
			log("Clear the table offline");
			this.clearOffline(db);
			
			for(int i=0; i<offlinesOld.size();i++){
				MegaOffline offline = offlinesOld.get(i);
				
				if(offline.getType()==null||offline.getType().equals("0")||offline.getType().equals("1")){
					log("Not encrypted: "+offline.getName());
					this.setOfflineFile(offline, db);	//using the method that encrypts								
				}
				else{
					log("Encrypted: "+offline.getName());
					this.setOfflineFileOld(offline, db);	//using the OLD method that doesn't encrypt	
				}
			}		
		}
		
		if(oldVersion <= 19){
			
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PIN_LOCK_TYPE + " TEXT;");			
			
			if(this.isPinLockEnabled(db)){
				log("PIN enabled!");
				db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_TYPE + " = '" + encrypt(Constants.PIN_4) + "';");
			}
			else{
				log("PIN NOT enabled!");
				db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_TYPE + " = '" + encrypt("") + "';");
			}			
		}

		if(oldVersion <= 20){
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_SORT_CLOUD + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_SORT_CONTACTS + " TEXT;");
			db.execSQL("ALTER TABLE " + TABLE_PREFERENCES + " ADD COLUMN " + KEY_PREFERRED_SORT_OTHERS + " TEXT;");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CLOUD + " = '" + encrypt(String.valueOf(MegaApiJava.ORDER_DEFAULT_ASC)) + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CONTACTS + " = '" + encrypt(String.valueOf(MegaApiJava.ORDER_DEFAULT_ASC)) + "';");
			db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_OTHERS + " = '" + encrypt(String.valueOf(MegaApiJava.ORDER_DEFAULT_ASC)) + "';");

			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_FILE_LOGGER_SDK + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_FILE_LOGGER_SDK + " = '" + encrypt("false") + "';");
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
					+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHAT_ENABLED + " BOOLEAN, " + KEY_CHAT_NOTIFICATIONS_ENABLED + " BOOLEAN, " +
					KEY_CHAT_SOUND_NOTIFICATIONS+ " TEXT, "+KEY_CHAT_VIBRATION_ENABLED+ " BOOLEAN"+")";
			db.execSQL(CREATE_CHAT_TABLE);
		}

		if (oldVersion <= 23){
			db.execSQL("ALTER TABLE " + TABLE_CREDENTIALS + " ADD COLUMN " + KEY_FIRST_NAME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_FIRST_NAME + " = '" + encrypt("") + "';");

			db.execSQL("ALTER TABLE " + TABLE_CREDENTIALS + " ADD COLUMN " + KEY_LAST_NAME + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_LAST_NAME + " = '" + encrypt("") + "';");
		}

		if (oldVersion <= 24){
			db.execSQL("ALTER TABLE " + TABLE_CHAT_SETTINGS + " ADD COLUMN " + KEY_CHAT_STATUS + " TEXT;");
			db.execSQL("UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_STATUS + " = '" + encrypt(MegaChatApi.STATUS_ONLINE+"") + "';");
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

		if (oldVersion <= 33){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_FILE_LOGGER_KARERE + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_FILE_LOGGER_KARERE + " = '" + encrypt("false") + "';");
		}

		if (oldVersion <= 34){
			db.execSQL("ALTER TABLE " + TABLE_ATTRIBUTES + " ADD COLUMN " + KEY_USE_HTTPS_ONLY + " TEXT;");
			db.execSQL("UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_USE_HTTPS_ONLY + " = '" + encrypt("false") + "';");
		}
	} 
	
//	public MegaOffline encrypt(MegaOffline off){
//		
//		off.setHandle(encrypt(off.getHandle()));
//		off.setPath(encrypt(off.getPath()));
//		off.setName(encrypt(off.getName()));
//		//Parent id no encrypted
//		off.setType(encrypt(off.getType()));
//		//incoming not encrypted
//		off.setHandleIncoming(encrypt(off.getHandleIncoming()));
//		
//		return off;
//	}
	
	public static String encrypt(String original) {
		if (original == null) {
			return null;
		}
		try {
			byte[] encrypted = Util.aes_encrypt(getAesKey(),original.getBytes());
			return Base64.encodeToString(encrypted, Base64.DEFAULT);
		} catch (Exception e) {
			log("ee");
			e.printStackTrace();
			return null;
		}
//		return original;
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
		log("saveEmail: "+email);
		String selectQuery = "SELECT * FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_CREDENTIALS_TABLE = "UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_EMAIL + "= '" + encrypt(email) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_CREDENTIALS_TABLE);
		}
		else{
			values.put(KEY_EMAIL, encrypt(email));
			db.insert(TABLE_CREDENTIALS, null, values);
		}
		cursor.close();
	}

	public void saveMyFirstName(String firstName) {

		String selectQuery = "SELECT * FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_CREDENTIALS_TABLE = "UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_FIRST_NAME + "= '" + encrypt(firstName) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_CREDENTIALS_TABLE);
		}
		else{
			values.put(KEY_FIRST_NAME, encrypt(firstName));
			db.insert(TABLE_CREDENTIALS, null, values);
		}
		cursor.close();
	}

	public void saveMyLastName(String lastName) {
		String selectQuery = "SELECT * FROM " + TABLE_CREDENTIALS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_CREDENTIALS_TABLE = "UPDATE " + TABLE_CREDENTIALS + " SET " + KEY_LAST_NAME + "= '" + encrypt(lastName) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_CREDENTIALS_TABLE);
		}
		else{
			values.put(KEY_LAST_NAME, encrypt(lastName));
			db.insert(TABLE_CREDENTIALS, null, values);
		}
		cursor.close();
	}
	
	public static String decrypt(String encodedString) {
		if (encodedString == null) {
			return null;
		}
		try {
			byte[] encoded = Base64.decode(encodedString, Base64.DEFAULT);
			byte[] original = Util.aes_decrypt(getAesKey(), encoded);
			return new String(original);
		} catch (Exception e) {
			log("de");
			return null;
		}
//		return encodedString;
	}
	
	public UserCredentials getCredentials(){
		UserCredentials userCredentials = null;
		
		String selectQuery = "SELECT  * FROM " + TABLE_CREDENTIALS;
		try{
			Cursor cursor = db.rawQuery(selectQuery, null);		
			if (cursor.moveToFirst()) {
				int id = Integer.parseInt(cursor.getString(0));
				String email = decrypt(cursor.getString(1));
				String session = decrypt(cursor.getString(2));
				String firstName = decrypt(cursor.getString(3));
				String lastName = decrypt(cursor.getString(4));
				String myHandle = decrypt(cursor.getString(5));
				userCredentials = new UserCredentials(email, session, firstName, lastName, myHandle);
			}
			cursor.close();
		}
		catch (SQLiteException e){
			if (db != null){
				onCreate(db);
			}
		}
        
        return userCredentials; 
	}

    public EphemeralCredentials getEphemeral(){
        EphemeralCredentials ephemeralCredentials = null;

        String selectQuery = "SELECT  * FROM " + TABLE_EPHEMERAL;
        try{
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                int id = Integer.parseInt(cursor.getString(0));
                String email = decrypt(cursor.getString(1));
                String password = decrypt(cursor.getString(2));
                String session = decrypt(cursor.getString(3));
                String firstName = decrypt(cursor.getString(4));
                String lastName = decrypt(cursor.getString(5));
                ephemeralCredentials = new EphemeralCredentials(email, password, session, firstName, lastName);
            }
            cursor.close();
        }
        catch (SQLiteException e){
            if (db != null){
                onCreate(db);
            }
        }

        return ephemeralCredentials;
    }

	public void updatePendingMessage(long idMessage, String temporalId, int sent) {

		ContentValues values = new ContentValues();
		values.put(KEY_ID_TEMP_KARERE, encrypt(temporalId));
		values.put(KEY_STATE, sent);
		String where = KEY_ID + "=" +idMessage;

		int rows = db.update(TABLE_PENDING_MSG, values, where, null);
		log("Rows updated: "+rows);
	}

	public void setFinishedPendingMessages() {
		log("setFinishedPendingMessages");

		ContentValues values = new ContentValues();
		values.put(KEY_STATE, PendingMessage.STATE_ERROR);
		String where = KEY_STATE + "=" +PendingMessage.STATE_SENDING;

		int rows = db.update(TABLE_PENDING_MSG, values, where, null);
		log("Rows updated: "+rows);
	}


	public void removePendingMessageById(long idMsg){

		ArrayList<Long> nodes = findMsgNodes(idMsg);
		if(nodes!=null){
			for(int i=0;i<nodes.size();i++){
				int rows = db.delete(TABLE_NODE_ATTACHMENTS, KEY_ID + "="+nodes.get(i), null);
				log("From TABLE_NODe_ATTACHMENTS deleted: "+rows);
			}
		}

		int rows = db.delete(TABLE_MSG_NODES, KEY_ID_PENDING_MSG + "="+idMsg, null);
		log("From TABLE_MSG_NODES deleted: "+rows);
		rows = db.delete(TABLE_PENDING_MSG, KEY_ID + "="+idMsg, null);
		log("From TABLE_PENDING_MSG deleted: "+rows);
	}

	public void removePendingMessageByChatId(long idChat){
		log("removePendingMessageByChatId");
		ArrayList<Long> messages = findIdPendingMessagesByChatId(idChat);
		if(messages!=null){
			for(int i=0;i<messages.size();i++){
				removePendingMessageById(messages.get(i));
			}
		}
	}

	public ArrayList<Long> findIdPendingMessagesByChatId(long idChat){
		log("findPendingMessageBySent");
		ArrayList<Long> idMessages = new ArrayList<>();
		String chat = idChat+"";

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG + " WHERE " +KEY_ID_CHAT + " ='"+ encrypt(chat)+"'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		ArrayList<Long> pendingIds = new ArrayList<>();
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){
				do{
					long id = cursor.getLong(0);

					idMessages.add(id);

				} while (cursor.moveToNext());
			}
		}
		cursor.close();
		return idMessages;
	}

	public ArrayList<AndroidMegaChatMessage> findAndroidMessagesBySent(int sent, long idChat){
		log("findPendingMessageBySent");
		ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
		String chat = idChat+"";

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG + " WHERE " + KEY_STATE + " = " + sent + " AND "+ KEY_ID_CHAT + " ='"+ encrypt(chat)+"'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		ArrayList<Long> pendingIds = new ArrayList<>();
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){
                do{
                    long id = cursor.getLong(0);
                    String timestamp = cursor.getString(2);
                    PendingMessage pendMsg = new PendingMessage(id, idChat, Long.valueOf(timestamp));

                    ArrayList<PendingNodeAttachment> nodes = findPendingNodesByMsgId(id);
					pendMsg.setState(sent);
                    pendMsg.setNodeAttachments(nodes);

                    AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(pendMsg, true);
                    messages.add(androidMsg);

                } while (cursor.moveToNext());
			}
		}
		cursor.close();
		return messages;
	}

	public ArrayList<AndroidMegaChatMessage> findAndroidMessagesNotSent(long idChat){
		log("findAndroidMessagesNotSent");
		ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();

		ArrayList<PendingMessage> pendMsgs = findPendingMessagesNotSent(idChat);

		for(int i=0;i<pendMsgs.size();i++){
			PendingMessage pendMsg = pendMsgs.get(i);
			long id = pendMsg.getId();
			ArrayList<PendingNodeAttachment> nodes = findPendingNodesByMsgId(id);
			pendMsg.setNodeAttachments(nodes);

			AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(pendMsg, true);
			messages.add(androidMsg);
		}

		log("Found: "+ messages.size());
		return messages;
	}

//	public ArrayList<AndroidMegaChatMessage> findPendingMessageById(long idMsg){
//		log("findPendingMessagesById");
////		ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
////
////		ArrayList<PendingMessage> pendMsgs = findPendingMessagesNotSent(idChat);
////
////		for(int i=0;i<pendMsgs.size();i++){
////			PendingMessage pendMsg = pendMsgs.get(i);
////			long id = pendMsg.getId();
////			ArrayList<PendingNodeAttachment> nodes = findPendingNodesByMsgId(id);
////			pendMsg.setNodeAttachments(nodes);
////
////			AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(pendMsg, true);
////			messages.add(androidMsg);
////		}
////
////		log("Found: "+ messages.size());
//		return messages;
//	}

	public ArrayList<PendingMessage> findPendingMessagesNotSent(long idChat) {
		log("findPendingMessagesNotSent");
		ArrayList<PendingMessage> pendMsgs = new ArrayList<>();
		String chat = idChat + "";

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG + " WHERE " + KEY_STATE + " < " + PendingMessage.STATE_SENT + " AND " + KEY_ID_CHAT + " ='" + encrypt(chat) + "'";
		log("QUERY: " + selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (!cursor.equals(null)) {
			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(0);
					String timestamp = decrypt(cursor.getString(2));
					long ts = Long.valueOf(timestamp);
					PendingMessage pendMsg = new PendingMessage(id, idChat, ts);
					pendMsg.setState(cursor.getInt(4));
					pendMsgs.add(pendMsg);

				} while (cursor.moveToNext());
			}
		}
		cursor.close();
		log("Found: "+pendMsgs.size());
		return pendMsgs;
	}

	public ArrayList<PendingNodeAttachment> findPendingNodesByMsgId(long idMsg){
		log("findPendingNodesByMsgId");
		ArrayList<PendingNodeAttachment> nodes = new ArrayList<>();

		String selectQuery = "SELECT * FROM " + TABLE_MSG_NODES + " WHERE " + KEY_ID_PENDING_MSG + " = " + idMsg;
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		ArrayList<Long> pendingIds = new ArrayList<>();
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){
				pendingIds.add(cursor.getLong(2));
			}
		}
		cursor.close();

		for(int i=0;i<pendingIds.size();i++){
            PendingNodeAttachment node = findPendingNodeById(pendingIds.get(i));
            if(node!=null){
                nodes.add(node);
            }

            else{
                log("Error the node is NULL");
            }
		}

		return nodes;
	}

	public PendingNodeAttachment findPendingNodeById(long idNode){
		log("findPendingNodeById");
        PendingNodeAttachment node = null;
        String selectQuery = "SELECT * FROM " + TABLE_NODE_ATTACHMENTS + " WHERE " + KEY_ID + " = " + idNode;
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){
				node = new PendingNodeAttachment(decrypt(cursor.getString(1)), decrypt(cursor.getString(3)), decrypt(cursor.getString(2)), Long.valueOf(cursor.getString(4)));
			}
		}
		cursor.close();
		return node;
	}

	public ArrayList<Long> findPendingMessagesBySent(int sent){
		log("findPendingMessageBySent");

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG + " WHERE " + KEY_STATE + " = " + sent;
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		ArrayList<Long> pendingIds = new ArrayList<>();
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){
				long id = cursor.getLong(0);
				pendingIds.add(id);
			}
		}
		cursor.close();
		log("Found: "+pendingIds.size());
		return pendingIds;
	}

	public void removeSentPendingMessages(){
		log("removeSentPendingMessages");
		ArrayList<Long> nodesToDelete = new ArrayList<>();
		ArrayList<Long> messages = findPendingMessagesBySent(PendingMessage.STATE_SENT);
		if(messages!=null){
			for(int i=0;i<messages.size();i++){
				ArrayList<Long> nodes = findMsgNodes(messages.get(i));
				if(nodes!=null){
					nodesToDelete.addAll(nodes);
				}
				int rows = db.delete(TABLE_MSG_NODES, KEY_ID_PENDING_MSG + "="+messages.get(i), null);
				log("From TABLE_MSG_NODES deleted: "+rows);
				rows = db.delete(TABLE_PENDING_MSG, KEY_ID + "="+messages.get(i), null);
				log("From TABLE_PENDING_MSG deleted: "+rows);

			}
		}

		for (int i=0; i<nodesToDelete.size();i++){
			int rows = db.delete(TABLE_NODE_ATTACHMENTS, KEY_ID + "="+nodesToDelete.get(i), null);
			log("From TABLE_NODE_ATTACHMENTS deleted: "+rows);
		}
	}

	public ArrayList<Long> findMsgNodes(long idMsg){
		log("findMsgNodes: "+idMsg);

		String selectQuery = "SELECT * FROM " + TABLE_MSG_NODES + " WHERE " + KEY_ID_PENDING_MSG + " = '" + idMsg + "'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);
		ArrayList<Long> nodes = new ArrayList<>();
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){
				do{
					long idNode = cursor.getLong(1);
					nodes.add(idNode);
				} while (cursor.moveToNext());
			}
		}

		cursor.close();

		return nodes;
	}

	public long setPendingMessage(String idChat, String timestamp){
		ContentValues values = new ContentValues();
		values.put(KEY_ID_CHAT, encrypt(idChat));
		values.put(KEY_MSG_TIMESTAMP, encrypt(timestamp));
		values.put(KEY_ID_TEMP_KARERE, -1+"");
		values.put(KEY_STATE, PendingMessage.STATE_SENDING);

		long id = db.insert(TABLE_PENDING_MSG, null, values);
		return id;
	}

	public long setNodeAttachment(String path, String name, String fingerprint){
		ContentValues values = new ContentValues();
		values.put(KEY_FILE_PATH, encrypt(path));
		values.put(KEY_FILE_NAME, encrypt(name));
		values.put(KEY_FILE_FINGERPRINT, encrypt(fingerprint));
		values.put(KEY_NODE_HANDLE, -1+"");

		long id = db.insert(TABLE_NODE_ATTACHMENTS, null, values);
		return id;
	}

	public long setMsgNode(long idMsg, long idNode){
		ContentValues values = new ContentValues();
		values.put(KEY_ID_PENDING_MSG, idMsg);
		values.put(KEY_ID_NODE, idNode);

		long id = db.insert(TABLE_MSG_NODES, null, values);
		return id;
	}

	public long findPendingMessageByIdTempKarere(long idTemp){
		log("findPendingMessageById: "+idTemp);
		String idPend = idTemp+"";
		long id = -1;

		String selectQuery = "SELECT * FROM " + TABLE_PENDING_MSG + " WHERE " + KEY_ID_TEMP_KARERE + " = '" + encrypt(idPend) + "'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){

				id = cursor.getLong(0);
			}
		}
		cursor.close();
		return id;
	}

	public void setPreferences (MegaPreferences prefs){
        ContentValues values = new ContentValues();
        values.put(KEY_FIRST_LOGIN, encrypt(prefs.getFirstTime()));
        values.put(KEY_CAM_SYNC_WIFI, encrypt(prefs.getCamSyncWifi()));
        values.put(KEY_CAM_SYNC_ENABLED, encrypt(prefs.getCamSyncEnabled()));
        values.put(KEY_CAM_SYNC_HANDLE, encrypt(prefs.getCamSyncHandle()));
        values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(prefs.getCamSyncLocalPath()));
        values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(prefs.getCamSyncFileUpload()));
        values.put(KEY_PIN_LOCK_ENABLED, encrypt(prefs.getPinLockEnabled()));
        values.put(KEY_PIN_LOCK_CODE, encrypt(prefs.getPinLockCode()));
        values.put(KEY_STORAGE_ASK_ALWAYS, encrypt(prefs.getStorageAskAlways()));
        values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(prefs.getStorageDownloadLocation()));
        values.put(KEY_CAM_SYNC_TIMESTAMP, encrypt(prefs.getCamSyncTimeStamp()));
        values.put(KEY_LAST_UPLOAD_FOLDER, encrypt(prefs.getLastFolderUpload()));
        values.put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(prefs.getLastFolderCloud()));
        values.put(KEY_SEC_FOLDER_ENABLED, encrypt(prefs.getSecondaryMediaFolderEnabled()));        
        values.put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(prefs.getLocalPathSecondaryFolder()));
        values.put(KEY_SEC_FOLDER_HANDLE, encrypt(prefs.getMegaHandleSecondaryFolder()));
        values.put(KEY_SEC_SYNC_TIMESTAMP, encrypt(prefs.getSecSyncTimeStamp())); 
        values.put(KEY_STORAGE_ADVANCED_DEVICES, encrypt(prefs.getStorageAdvancedDevices()));
        values.put(KEY_PREFERRED_VIEW_LIST, encrypt(prefs.getPreferredViewList()));
        values.put(KEY_PREFERRED_VIEW_LIST_CAMERA, encrypt(prefs.getPreferredViewListCameraUploads()));
        values.put(KEY_URI_EXTERNAL_SD_CARD, encrypt(prefs.getUriExternalSDCard()));
        values.put(KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD, encrypt(prefs.getCameraFolderExternalSDCard()));
        values.put(KEY_PIN_LOCK_TYPE, encrypt(prefs.getPinLockType()));
		values.put(KEY_PREFERRED_SORT_CLOUD, encrypt(prefs.getPreferredSortCloud()));
		values.put(KEY_PREFERRED_SORT_CONTACTS, encrypt(prefs.getPreferredSortContacts()));
		values.put(KEY_PREFERRED_SORT_OTHERS, encrypt(prefs.getPreferredSortOthers()));
		values.put(KEY_FIRST_LOGIN_CHAT, encrypt(prefs.getFirstTimeChat()));
        db.insert(TABLE_PREFERENCES, null, values);
	}
	
	public MegaPreferences getPreferences(){
		log("getPreferences");
		MegaPreferences prefs = null;
		
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			int id = Integer.parseInt(cursor.getString(0));
			String firstTime = decrypt(cursor.getString(1));
			String camSyncEnabled = decrypt(cursor.getString(2));
			String camSyncHandle = decrypt(cursor.getString(3));
			String camSyncLocalPath = decrypt(cursor.getString(4));
			String wifi = decrypt(cursor.getString(5));
			String fileUpload = decrypt(cursor.getString(6));
			String pinLockEnabled = decrypt(cursor.getString(7));
			String pinLockCode = decrypt(cursor.getString(8));
			String askAlways = decrypt(cursor.getString(9));
			String downloadLocation = decrypt(cursor.getString(10));
			String camSyncTimeStamp = decrypt(cursor.getString(11));
			String camSyncCharging = decrypt(cursor.getString(12));
			String lastFolderUpload = decrypt(cursor.getString(13));
			String lastFolderCloud = decrypt(cursor.getString(14));
			String secondaryFolderEnabled = decrypt(cursor.getString(15));
			String secondaryPath = decrypt(cursor.getString(16));
			String secondaryHandle = decrypt(cursor.getString(17));
			String secSyncTimeStamp = decrypt(cursor.getString(18));
			String keepFileNames = decrypt(cursor.getString(19));
			String storageAdvancedDevices= decrypt(cursor.getString(20));
			String preferredViewList = decrypt(cursor.getString(21));
			String preferredViewListCamera = decrypt(cursor.getString(22));
			String uriExternalSDCard = decrypt(cursor.getString(23));
			String cameraFolderExternalSDCard = decrypt(cursor.getString(24));
			String pinLockType = decrypt(cursor.getString(25));
			String preferredSortCloud = decrypt(cursor.getString(26));
			String preferredSortContacts = decrypt(cursor.getString(27));
			String preferredSortOthers = decrypt(cursor.getString(28));
			String firstTimeChat = decrypt(cursor.getString(29));
			
			prefs = new MegaPreferences(firstTime, wifi, camSyncEnabled, camSyncHandle, camSyncLocalPath, fileUpload, camSyncTimeStamp, pinLockEnabled, 
					pinLockCode, askAlways, downloadLocation, camSyncCharging, lastFolderUpload, lastFolderCloud, secondaryFolderEnabled, secondaryPath, secondaryHandle, 
					secSyncTimeStamp, keepFileNames, storageAdvancedDevices, preferredViewList, preferredViewListCamera, uriExternalSDCard, cameraFolderExternalSDCard,
					pinLockType, preferredSortCloud, preferredSortContacts, preferredSortOthers, firstTimeChat);
		}
		cursor.close();
		
		return prefs;
	}

	public ChatSettings getChatSettings(){
		log("getChatSettings");
		ChatSettings chatSettings = null;

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			int id = Integer.parseInt(cursor.getString(0));
			String enabled = decrypt(cursor.getString(1));
			String notificationsEnabled = decrypt(cursor.getString(2));
			String notificationSound = decrypt(cursor.getString(3));
			String vibrationEnabled = decrypt(cursor.getString(4));
			String chatStatus = decrypt(cursor.getString(5));
			chatSettings = new ChatSettings(enabled, notificationsEnabled, notificationSound, vibrationEnabled);
		}
		cursor.close();

		return chatSettings;
	}

	public void setChatSettings(ChatSettings chatSettings){
		log("setChatSettings");

        db.execSQL("DELETE FROM " + TABLE_CHAT_SETTINGS);

		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_ENABLED, encrypt(chatSettings.getEnabled()));
		values.put(KEY_CHAT_NOTIFICATIONS_ENABLED, encrypt(chatSettings.getNotificationsEnabled()));
		values.put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(chatSettings.getNotificationsSound()));
		values.put(KEY_CHAT_VIBRATION_ENABLED, encrypt(chatSettings.getVibrationEnabled()));

		db.insert(TABLE_CHAT_SETTINGS, null, values);
	}

	public void setEnabledChat(String enabled){
		log("setEnabledChat");

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_ENABLED + "= '" + encrypt(enabled) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_CHAT_ENABLED, encrypt(enabled));
			db.insert(TABLE_CHAT_SETTINGS, null, values);
		}
		cursor.close();
	}

	public void setNotificationEnabledChat(String enabled){

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_NOTIFICATIONS_ENABLED + "= '" + encrypt(enabled) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_CHAT_NOTIFICATIONS_ENABLED, encrypt(enabled));
			db.insert(TABLE_CHAT_SETTINGS, null, values);
		}
		cursor.close();
	}

	public void setNotificationSoundChat(String sound){
		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_SOUND_NOTIFICATIONS + "= '" + encrypt(sound) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(sound));
			db.insert(TABLE_CHAT_SETTINGS, null, values);
		}
		cursor.close();
	}

	public void setVibrationEnabledChat(String enabled){
		String selectQuery = "SELECT * FROM " + TABLE_CHAT_SETTINGS;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_CHAT_SETTINGS + " SET " + KEY_CHAT_VIBRATION_ENABLED + "= '" + encrypt(enabled) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_CHAT_VIBRATION_ENABLED, encrypt(enabled));
			db.insert(TABLE_CHAT_SETTINGS, null, values);
		}
		cursor.close();
	}

	public void setChatItemPreferences(ChatItemPreferences chatPrefs){
		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_HANDLE, encrypt(chatPrefs.getChatHandle()));
		values.put(KEY_CHAT_ITEM_NOTIFICATIONS, encrypt(chatPrefs.getNotificationsEnabled()));
		values.put(KEY_CHAT_ITEM_RINGTONE, encrypt(chatPrefs.getRingtone()));
		values.put(KEY_CHAT_ITEM_SOUND_NOTIFICATIONS, encrypt(chatPrefs.getNotificationsSound()));

		db.insert(TABLE_CHAT_ITEMS, null, values);
	}

	public int setRingtoneChatItem(String ringtone, String handle){
		log("setRingtoneChatItem: "+ringtone+" "+handle);

		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_ITEM_RINGTONE, encrypt(ringtone));
		return db.update(TABLE_CHAT_ITEMS, values, KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'", null);
	}

	public int setNotificationSoundChatItem(String sound, String handle){
		log("setNotificationSoundChatItem: "+sound+" "+handle);

		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_ITEM_SOUND_NOTIFICATIONS, encrypt(sound));
		return db.update(TABLE_CHAT_ITEMS, values, KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'", null);
	}

	public int setNotificationEnabledChatItem(String enabled, String handle){
		log("setNotificationEnabledChatItem: "+enabled+" "+handle);

		ContentValues values = new ContentValues();
		values.put(KEY_CHAT_ITEM_NOTIFICATIONS, encrypt(enabled));
		return db.update(TABLE_CHAT_ITEMS, values, KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'", null);
	}

	public ChatItemPreferences findChatPreferencesByHandle (String handle){
		log("findChatPreferencesByHandle: "+handle);
		ChatItemPreferences prefs = null;

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_ITEMS + " WHERE " + KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){

				int id = Integer.parseInt(cursor.getString(0));
				String chatHandle = decrypt(cursor.getString(1));
				String notificationsEnabled = decrypt(cursor.getString(2));
				log("notificationsEnabled: "+notificationsEnabled);
				String ringtone = decrypt(cursor.getString(3));
				String notificationsSound = decrypt(cursor.getString(4));

				prefs = new ChatItemPreferences(chatHandle, notificationsEnabled, ringtone, notificationsSound);
				cursor.close();
				return prefs;
			}
		}
		cursor.close();
		return null;
	}

	public boolean areNotificationsEnabled (String handle){
		log("areNotificationsEnabled: "+handle);

		String selectQuery = "SELECT * FROM " + TABLE_CHAT_ITEMS + " WHERE " + KEY_CHAT_HANDLE + " = '" + encrypt(handle) + "'";
		Cursor cursor = db.rawQuery(selectQuery, null);
		boolean result = true;
		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){

				String notificationsEnabled = decrypt(cursor.getString(2));
				boolean muteB = Boolean.parseBoolean(notificationsEnabled);
				if(muteB==true){
					result = true;
				}
				else{
					result = false;
				}
			}
		}

		cursor.close();
		return result;
	}

	public void setCompletedTransfer(AndroidCompletedTransfer transfer){
		ContentValues values = new ContentValues();
		values.put(KEY_TRANSFER_FILENAME, encrypt(transfer.getFileName()));
		values.put(KEY_TRANSFER_TYPE, encrypt(transfer.getType()+""));
		values.put(KEY_TRANSFER_STATE, encrypt(transfer.getState()+""));
		values.put(KEY_TRANSFER_SIZE, encrypt(transfer.getSize()));
		values.put(KEY_TRANSFER_HANDLE, encrypt(transfer.getNodeHandle()));

		db.insert(TABLE_COMPLETED_TRANSFERS, null, values);
	}

	public void emptyCompletedTransfers(){
		db.delete(TABLE_COMPLETED_TRANSFERS, null,null);
	}

	public ArrayList<AndroidCompletedTransfer> getCompletedTransfers(){
		ArrayList<AndroidCompletedTransfer> cTs = new ArrayList<AndroidCompletedTransfer> ();

		String selectQuery = "SELECT * FROM " + TABLE_COMPLETED_TRANSFERS;
		Cursor cursor = db.rawQuery(selectQuery, null);
		try {
			if (cursor.moveToLast()){

				do {
					int id = Integer.parseInt(cursor.getString(0));
					String filename = decrypt(cursor.getString(1));
					String type =  decrypt(cursor.getString(2));
					int typeInt = Integer.parseInt(type);
					String state = decrypt(cursor.getString(3));
					int stateInt = Integer.parseInt(state);
					String size = decrypt(cursor.getString(4));
					String nodeHandle = decrypt(cursor.getString(5));

					AndroidCompletedTransfer cT = new AndroidCompletedTransfer(filename, typeInt, stateInt, size, nodeHandle);
					cTs.add(cT);
				} while (cursor.moveToPrevious());
			}

		} finally {
			try { cursor.close(); } catch (Exception ignore) {}
		}

		return cTs;
	}


	public boolean isPinLockEnabled(SQLiteDatabase db){
		log("getPinLockEnabled");
		
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		Cursor cursor = db.rawQuery(selectQuery, null);
		String pinLockEnabled = null;
		boolean result = false;
		if (cursor.moveToFirst()){
			//get pinLockEnabled
			pinLockEnabled = decrypt(cursor.getString(7));	
			if (pinLockEnabled == null){
				result = false;
			}
			else{
				if(pinLockEnabled.equals("true")){
					result = true;
				}
				else{
					result = false;
				}
			}
		}
		cursor.close();
		
		return result;
	}

	
	public void setAttributes (MegaAttributes attr){
		log("setAttributes");
        ContentValues values = new ContentValues();
        values.put(KEY_ATTR_ONLINE, encrypt(attr.getOnline()));
        values.put(KEY_ATTR_INTENTS, encrypt(Integer.toString(attr.getAttemps())));
        values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(attr.getAskSizeDownload()));
        values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(attr.getAskNoAppDownload()));
		values.put(KEY_FILE_LOGGER_SDK, encrypt(attr.getFileLoggerSDK()));
		values.put(KEY_ACCOUNT_DETAILS_TIMESTAMP, encrypt(attr.getAccountDetailsTimeStamp()));
		values.put(KEY_PAYMENT_METHODS_TIMESTAMP, encrypt(attr.getPaymentMethodsTimeStamp()));
		values.put(KEY_PRICING_TIMESTAMP, encrypt(attr.getPricingTimeStamp()));
		values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP, encrypt(attr.getExtendedAccountDetailsTimeStamp()));
		values.put(KEY_INVALIDATE_SDK_CACHE, encrypt(attr.getInvalidateSdkCache()));
		values.put(KEY_FILE_LOGGER_KARERE, encrypt(attr.getFileLoggerKarere()));
		values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.getUseHttpsOnly()));
		db.insert(TABLE_ATTRIBUTES, null, values);
	}
	
	public MegaAttributes getAttributes(){
		MegaAttributes attr = null;
		
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			int id = Integer.parseInt(cursor.getString(0));
			String online = decrypt(cursor.getString(1));
			String intents =  decrypt(cursor.getString(2));
			String askSizeDownload = decrypt(cursor.getString(3));
			String askNoAppDownload = decrypt(cursor.getString(4));
			String fileLoggerSDK = decrypt(cursor.getString(5));
			String accountDetailsTimeStamp = decrypt(cursor.getString(6));
			String paymentMethodsTimeStamp = decrypt(cursor.getString(7));
			String pricingTimeStamp = decrypt(cursor.getString(8));
			String extendedAccountDetailsTimeStamp = decrypt(cursor.getString(9));
			String invalidateSdkCache = decrypt(cursor.getString(10));
			String fileLoggerKarere = decrypt(cursor.getString(11));
			String useHttpsOnly = decrypt(cursor.getString(12));
			if(intents!=null){
				attr = new MegaAttributes(online, Integer.parseInt(intents), askSizeDownload, askNoAppDownload, fileLoggerSDK, accountDetailsTimeStamp, paymentMethodsTimeStamp, pricingTimeStamp, extendedAccountDetailsTimeStamp, invalidateSdkCache, fileLoggerKarere, useHttpsOnly);
			}
			else{
				attr = new MegaAttributes(online, 0, askSizeDownload, askNoAppDownload, fileLoggerSDK, accountDetailsTimeStamp, paymentMethodsTimeStamp, pricingTimeStamp, extendedAccountDetailsTimeStamp, invalidateSdkCache, fileLoggerKarere, useHttpsOnly);
			}
		}
		cursor.close();
		
		return attr;
	}

//	public void setNonContact (NonContactInfo nonContact){
//		log("setNonContact: "+nonContact.getHandle());
//
//		ContentValues values = new ContentValues();
//		values.put(KEY_NONCONTACT_HANDLE,  encrypt(nonContact.getHandle()));
//		values.put(KEY_NONCONTACT_FULLNAME, encrypt(nonContact.getFullName()));
//		values.put(KEY_NONCONTACT_FIRSTNAME, encrypt(nonContact.getFirstName()));
//		values.put(KEY_NONCONTACT_LASTNAME, encrypt(nonContact.getLastName()));
//
//		NonContactInfo check = findNonContactByHandle(nonContact.getHandle()+"");
//
//		if(check==null){
//			db.insert(TABLE_NON_CONTACTS, null, values);
//		}
//		else{
//			int id = (int) db.insertWithOnConflict(TABLE_NON_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//			log("setNonContact: Final value: "+id);
//		}
//	}

	public int setNonContactFirstName (String name, String handle){
		log("setContactName: "+name+" "+handle);

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

//	public int setNonContactFullName (String fullName, String handle){
//		log("setNonContactFullName: "+fullName);
//
//		ContentValues values = new ContentValues();
//		values.put(KEY_NONCONTACT_FULLNAME, encrypt(fullName));
//		return db.update(TABLE_NON_CONTACTS, values, KEY_NONCONTACT_FULLNAME + " = '" + encrypt(handle) + "'", null);
//	}

	public NonContactInfo findNonContactByHandle(String handle){
		log("findNONContactByHandle: "+handle);
		NonContactInfo noncontact = null;

		String selectQuery = "SELECT * FROM " + TABLE_NON_CONTACTS + " WHERE " + KEY_NONCONTACT_HANDLE + " = '" + encrypt(handle)+ "'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){

				int _id = Integer.parseInt(cursor.getString(0));
				String _handle = decrypt(cursor.getString(1));
				String _fullName = decrypt(cursor.getString(2));
				String _firstName = decrypt(cursor.getString(3));
				String _lastName = decrypt(cursor.getString(4));
				String _email = decrypt(cursor.getString(5));

				noncontact = new NonContactInfo(handle, _fullName, _firstName, _lastName, _email);
				cursor.close();
				return noncontact;
			}
		}
		cursor.close();
		return null;
	}
	
	public void setContact (MegaContactDB contact){
		log("setContacts: "+contact.getMail());
        ContentValues values = new ContentValues();
        values.put(KEY_CONTACT_HANDLE, encrypt(contact.getHandle()));
        values.put(KEY_CONTACT_MAIL, encrypt(contact.getMail()));
        values.put(KEY_CONTACT_NAME, encrypt(contact.getName()));
        values.put(KEY_CONTACT_LAST_NAME, encrypt(contact.getLastName()));
//        values.put(KEY_CONTACT_HANDLE, (contacts.getHandle()));
//        values.put(KEY_CONTACT_MAIL, (contacts.getMail()));
//        values.put(KEY_CONTACT_NAME, (contacts.getName()));
//        values.put(KEY_CONTACT_LAST_NAME, (contacts.getLastName()));
		db.insert(TABLE_CONTACTS, null, values);
	}
	
	public int setContactName (String name, String mail){
		log("setContactName: "+name+" "+mail);
		
		ContentValues values = new ContentValues();
	    values.put(KEY_CONTACT_NAME, encrypt(name));
	    return db.update(TABLE_CONTACTS, values, KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'", null);
	}
	
	public int setContactLastName (String lastName, String mail){
		
		ContentValues values = new ContentValues();
	    values.put(KEY_CONTACT_LAST_NAME, encrypt(lastName));
	    return db.update(TABLE_CONTACTS, values, KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'", null);
	}
	
	public int getContactsSize(){
		String selectQuery = "SELECT * FROM " + TABLE_CONTACTS;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor != null){
			return cursor.getCount();
		}
		else{
			return 0;
		}
	}

	public int setContactMail (long handle, String mail){
		log("setContactMail: "+handle+" "+mail);

		ContentValues values = new ContentValues();
		values.put(KEY_CONTACT_MAIL, encrypt(mail));
		return db.update(TABLE_CONTACTS, values, KEY_CONTACT_HANDLE + " = '" + encrypt(String.valueOf(handle)) + "'", null);
	}
	
	public MegaContactDB findContactByHandle(String handle){
		log("findContactByHandle: "+handle);
		MegaContactDB contacts = null;

		String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_CONTACT_HANDLE + " = '" + encrypt(handle) + "'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				String _handle = null;
				String _mail = null;
				String _name = null;
				String _lastName = null;
				
				_id = Integer.parseInt(cursor.getString(0));
				_handle = decrypt(cursor.getString(1));
				_mail = decrypt(cursor.getString(2));
				_name = decrypt(cursor.getString(3));
				_lastName = decrypt(cursor.getString(4));

				contacts = new MegaContactDB(handle, _mail, _name, _lastName);
				cursor.close();
				return contacts;
			}
		}
		cursor.close();
		return null;		
	}

	public MegaContactDB findContactByEmail(String mail){
		log("findContactByEmail: "+mail);
		MegaContactDB contacts = null;

		String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'";
		log("QUERY: "+selectQuery);
		Cursor cursor = db.rawQuery(selectQuery, null);

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){

				int _id = -1;
				String _handle = null;
				String _mail = null;
				String _name = null;
				String _lastName = null;

				_id = Integer.parseInt(cursor.getString(0));
				_handle = decrypt(cursor.getString(1));
				_mail = decrypt(cursor.getString(2));
				_name = decrypt(cursor.getString(3));
				_lastName = decrypt(cursor.getString(4));

				contacts = new MegaContactDB(_handle, mail, _name, _lastName);
				cursor.close();
				return contacts;
			}
		}
		cursor.close();
		return null;
	}
	
	public long setOfflineFile (MegaOffline offline){
		log("setOfflineFile: "+offline.getHandle());
        ContentValues values = new ContentValues();
        
        MegaOffline checkInsert = null;
        checkInsert=findByHandle(offline.getHandle());              
        
        if(checkInsert==null){
        	String nullColumnHack = null;        	
            
            values.put(KEY_OFF_HANDLE, encrypt(offline.getHandle()));
            values.put(KEY_OFF_PATH, encrypt(offline.getPath()));
            values.put(KEY_OFF_NAME, encrypt(offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getparentId());
            values.put(KEY_OFF_TYPE, encrypt(offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.getHandleIncoming()));
            
            long ret = db.insert(TABLE_OFFLINE, nullColumnHack, values);
            
            return ret;        	
        }
        return -1;
	}
	
	public long setOfflineFile (MegaOffline offline, SQLiteDatabase db){
		
        ContentValues values = new ContentValues();
        
        MegaOffline checkInsert = null;
        checkInsert=findByHandle(offline.getHandle(),db);              
        
        if(checkInsert==null){
        	String nullColumnHack = null;        	
            
            values.put(KEY_OFF_HANDLE, encrypt(offline.getHandle()));
            values.put(KEY_OFF_PATH, encrypt(offline.getPath()));
            values.put(KEY_OFF_NAME, encrypt(offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getparentId());
            values.put(KEY_OFF_TYPE, encrypt(offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.getHandleIncoming()));
            
            long ret = db.insert(TABLE_OFFLINE, nullColumnHack, values);
            
            return ret;        	
        }
        return -1;
	}
	
	public long setOfflineFileOld (MegaOffline offline){
		
        ContentValues values = new ContentValues();
        
        MegaOffline checkInsert = null;
        checkInsert=findByHandle(offline.getHandle(),db);              
        
        if(checkInsert==null){
        	String nullColumnHack = null;        	
            
            values.put(KEY_OFF_HANDLE, (offline.getHandle()));
            values.put(KEY_OFF_PATH, (offline.getPath()));
            values.put(KEY_OFF_NAME, (offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getparentId());
            values.put(KEY_OFF_TYPE, (offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, (offline.getHandleIncoming()));
            
            long ret = db.insert(TABLE_OFFLINE, nullColumnHack, values);
            
            return ret;        	
        }
        return -1;
	}
	
	public long setOfflineFileOld (MegaOffline offline, SQLiteDatabase db){
		
        ContentValues values = new ContentValues();
        
        MegaOffline checkInsert = null;
        checkInsert=findByHandle(offline.getHandle(), db);              
        
        if(checkInsert==null){
        	String nullColumnHack = null;        	
            
            values.put(KEY_OFF_HANDLE, (offline.getHandle()));
            values.put(KEY_OFF_PATH, (offline.getPath()));
            values.put(KEY_OFF_NAME, (offline.getName()));
            values.put(KEY_OFF_PARENT, offline.getparentId());
            values.put(KEY_OFF_TYPE, (offline.getType()));
            values.put(KEY_OFF_INCOMING, offline.getOrigin());
            values.put(KEY_OFF_HANDLE_INCOMING, (offline.getHandleIncoming()));
            
            long ret = db.insert(TABLE_OFFLINE, nullColumnHack, values);
            
            return ret;        	
        }
        return -1;
	}
		
	public ArrayList<MegaOffline> getOfflineFiles (){
		
		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			do{
				
				int id = Integer.parseInt(cursor.getString(0));
				String handle = decrypt(cursor.getString(1));
				String path = decrypt(cursor.getString(2));
				String name = decrypt(cursor.getString(3));
				int parent = cursor.getInt(4);				
				String type = decrypt(cursor.getString(5));				
				int incoming = cursor.getInt(6);
				String handleIncoming = decrypt(cursor.getString(7));
				MegaOffline offline = new MegaOffline(id,handle, path, name, parent, type, incoming, handleIncoming);
				listOffline.add(offline);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return listOffline;
	}
	
	public ArrayList<MegaOffline> getOfflineFilesOld (SQLiteDatabase db){
		
		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			do{
				
				int id = Integer.parseInt(cursor.getString(0));
				String handle = (cursor.getString(1));
				String path = (cursor.getString(2));
				String name = (cursor.getString(3));
				int parent = cursor.getInt(4);				
				String type = (cursor.getString(5));				
				int incoming = cursor.getInt(6);
				String handleIncoming = (cursor.getString(7));
				MegaOffline offline = new MegaOffline(id,handle, path, name, parent, type, incoming, handleIncoming);
				listOffline.add(offline);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return listOffline;
	}

	public boolean exists(long handle){
				
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + encrypt(Long.toString(handle)) + "'";
		
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		if (!cursor.equals(null)){
		
			boolean r = cursor.moveToFirst();
			cursor.close();
			
			return r;
		}	        
		
		cursor.close();
		
		return false; 		 
	}
	
	public MegaOffline findByHandle(long handle){
		log("findByHandle: "+handle);

		MegaOffline offline = null;
		
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + encrypt(String.valueOf(handle)) + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;
				int _incoming = 0;
				String _handleIncoming = null;

				_id = Integer.parseInt(cursor.getString(0));
				_handle = decrypt(cursor.getString(1));
				_path = decrypt(cursor.getString(2));
				_name = decrypt(cursor.getString(3));
				_parent = cursor.getInt(4);
				_type = decrypt(cursor.getString(5));
				_incoming = cursor.getInt(6);
				_handleIncoming = decrypt(cursor.getString(7));
				offline = new MegaOffline(_id,_handle, _path, _name, _parent, _type, _incoming, _handleIncoming);
				cursor.close();
				return offline;
			}
		}
		cursor.close();
		return null; 		 
	}
	
	public MegaOffline findByHandle(String handle){

		MegaOffline offline = null;
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + encrypt(handle) + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;
				int _incoming = 0;
				String _handleIncoming = null;
				
				_id = Integer.parseInt(cursor.getString(0));
				_handle = decrypt(cursor.getString(1));
				_path = decrypt(cursor.getString(2));
				_name = decrypt(cursor.getString(3));
				_parent = cursor.getInt(4);
				_type = decrypt(cursor.getString(5));
				_incoming = cursor.getInt(6);
				_handleIncoming = decrypt(cursor.getString(7));
				
				offline = new MegaOffline(_id,_handle, _path, _name, _parent, _type,  _incoming, _handleIncoming);
				cursor.close();
				return offline;
			}
		}
		cursor.close();
		return null;
		
	}
	
	public MegaOffline findByHandle(String handle, SQLiteDatabase db){

		MegaOffline offline = null;
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + encrypt(handle) + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;
				int _incoming = 0;
				String _handleIncoming = null;
				
				_id = Integer.parseInt(cursor.getString(0));
				_handle = decrypt(cursor.getString(1));
				_path = decrypt(cursor.getString(2));
				_name = decrypt(cursor.getString(3));
				_parent = cursor.getInt(4);
				_type = decrypt(cursor.getString(5));
				_incoming = cursor.getInt(6);
				_handleIncoming = decrypt(cursor.getString(7));
				
				offline = new MegaOffline(_id,_handle, _path, _name, _parent, _type,  _incoming, _handleIncoming);
				cursor.close();
				return offline;
			}
		}
		cursor.close();
		return null;
		
	}
	
	public ArrayList<MegaOffline> findByParentId(int parentId){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PARENT + " = '" + parentId + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		
				do{
					int _id = -1;
					int _parent = -1;
					String _handle = null;
					String _path = null;
					String _name = null;
					String _type = null;
					int _incoming = 0;
					String _handleIncoming = null;
					
					_id = Integer.parseInt(cursor.getString(0));
					_handle = decrypt(cursor.getString(1));
					_path = decrypt(cursor.getString(2));
					_name = decrypt(cursor.getString(3));
					_parent = cursor.getInt(4);
					_type = decrypt(cursor.getString(5));
					_incoming = cursor.getInt(6);
					_handleIncoming = decrypt(cursor.getString(7));
					
					listOffline.add(new MegaOffline(_id,_handle, _path, _name, _parent, _type, _incoming, _handleIncoming));
				} while (cursor.moveToNext());
			}
		}
		
		cursor.close();
		return listOffline; 		 
	}
	
	public MegaOffline findById(int id){		
		
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_ID + " = '" + id + "'";
		MegaOffline mOffline = null;
		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		
				do{
					int _id = -1;
					int _parent = -1;
					String _handle = null;
					String _path = null;
					String _name = null;
					String _type = null;
					int _incoming = 0;
					String _handleIncoming = null;
					
					_id = Integer.parseInt(cursor.getString(0));
					_handle = decrypt(cursor.getString(1));
					_path = decrypt(cursor.getString(2));
					_name = decrypt(cursor.getString(3));
					_parent = cursor.getInt(4);
					_type = decrypt(cursor.getString(5));
					_incoming = cursor.getInt(6);
					_handleIncoming = decrypt(cursor.getString(7));
					
					mOffline = new MegaOffline (_id,_handle, _path, _name, _parent, _type, _incoming, _handleIncoming);
					
				} while (cursor.moveToNext());
			}
		}
		
		cursor.close();
		
		return mOffline; 		 
	}
	
	public int removeById(int id){	

		return db.delete(TABLE_OFFLINE, KEY_ID + "="+id, null);		
		
	}	
	
	public ArrayList<MegaOffline> findByPath(String path){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + encrypt(path) + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		
				do{
					int _id = -1;
					int _parent = -1;
					String _handle = null;
					String _path = null;
					String _name = null;
					String _type = null;
					int _incoming = 0;
					String _handleIncoming = null;
	
					_id = Integer.parseInt(cursor.getString(0));
					_handle = decrypt(cursor.getString(1));
					_path = decrypt(cursor.getString(2));
					_name = decrypt(cursor.getString(3));
					_parent = cursor.getInt(4);
					_type = decrypt(cursor.getString(5));
					_incoming = cursor.getInt(6);
					_handleIncoming = decrypt(cursor.getString(7));
					
					listOffline.add(new MegaOffline(_id,_handle, _path, _name, _parent, _type, _incoming, _handleIncoming));
				} while (cursor.moveToNext());
			}
		}
		cursor.close();
		return listOffline; 		 
	}
	
	public MegaOffline findbyPathAndName(String path, String name){
		
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + encrypt(path) + "'" + "AND " + KEY_OFF_NAME + " = '" + encrypt(name) + "'"  ;
		
		MegaOffline mOffline = null;
		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		
				do{
					int _id = -1;
					int _parent = -1;
					String _handle = null;
					String _path = null;
					String _name = null;
					String _type = null;
					int _incoming = 0;
					String _handleIncoming = null;
					
					_id = Integer.parseInt(cursor.getString(0));
					_handle = decrypt(cursor.getString(1));
					_path = decrypt(cursor.getString(2));
					_name = decrypt(cursor.getString(3));
					_parent = cursor.getInt(4);
					_type = decrypt(cursor.getString(5));
					_incoming = cursor.getInt(6);
					_handleIncoming = decrypt(cursor.getString(7));
					
					mOffline = new MegaOffline (_id,_handle, _path, _name, _parent, _type, _incoming, _handleIncoming);
					
				} while (cursor.moveToNext());
			}
		}
		cursor.close();
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
		
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		if (cursor.moveToFirst()){			
				
			_id = Integer.parseInt(cursor.getString(0));
			_handle = decrypt(cursor.getString(1));
			_path = decrypt(cursor.getString(2));
			_name = decrypt(cursor.getString(3));
			_parent = cursor.getInt(4);
			_type = decrypt(cursor.getString(5));
			_incoming = cursor.getInt(6);
			_handleIncoming = cursor.getString(7);
		}
		
		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		
		//Get the rest of nodes with the same parent (if there be)
		if(_parent!=-1){
			
			selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PARENT + " = '" + _parent + "'";
			
			cursor = db.rawQuery(selectQuery, null);
			if (cursor.moveToFirst()){
				do{
					
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
			cursor.close();
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
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_FIRST_LOGIN + "= '" + encrypt(firstTime + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_FIRST_LOGIN, encrypt(firstTime + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}

	public void setFirstTimeChat (boolean firstTimeChat){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_FIRST_LOGIN_CHAT + "= '" + encrypt(firstTimeChat + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_FIRST_LOGIN_CHAT, encrypt(firstTimeChat + ""));
			db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	
	public void setCamSyncWifi (boolean wifi){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_WIFI + "= '" + encrypt(wifi + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_WIFI, encrypt(wifi + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setPreferredViewList (boolean list){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_VIEW_LIST + "= '" + encrypt(list + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PREFERRED_VIEW_LIST, encrypt(list + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setPreferredViewListCamera (boolean list){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_VIEW_LIST_CAMERA + "= '" + encrypt(list + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PREFERRED_VIEW_LIST_CAMERA, encrypt(list + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}

	public void setPreferredSortCloud (String order){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CLOUD + "= '" + encrypt(order) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_PREFERRED_SORT_CLOUD, encrypt(order));
			db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}

	public void setPreferredSortContacts (String order){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_CONTACTS + "= '" + encrypt(order) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_PREFERRED_SORT_CONTACTS, encrypt(order));
			db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}

	public void setPreferredSortOthers (String order){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PREFERRED_SORT_OTHERS + "= '" + encrypt(order) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_PREFERRED_SORT_OTHERS, encrypt(order));
			db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setLastUploadFolder (String folderPath){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_LAST_UPLOAD_FOLDER + "= '" + encrypt(folderPath + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE UPLOAD FOLDER: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_LAST_UPLOAD_FOLDER, encrypt(folderPath + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setLastCloudFolder (String folderHandle){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_LAST_CLOUD_FOLDER_HANDLE + "= '" + encrypt(folderHandle + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("KEY_LAST_CLOUD_FOLDER_HANDLE UPLOAD FOLDER: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(folderHandle + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	
	public void setCamSyncCharging (boolean charging){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_CHARGING + "= '" + encrypt(charging + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC CHARGING: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_CHARGING, encrypt(charging + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setKeepFileNames (boolean charging){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_KEEP_FILE_NAMES + "= '" + encrypt(charging + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC KEEP_FILES: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_KEEP_FILE_NAMES, encrypt(charging + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setCamSyncEnabled (boolean enabled){
		log("setCamSyncEnabled: "+enabled);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_ENABLED + "= '" + encrypt(enabled + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_ENABLED, encrypt(enabled + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setSecondaryUploadEnabled (boolean enabled){
		log("setSecondaryUploadEnabled: "+enabled);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_ENABLED + "= '" + encrypt(enabled + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);			
		}
		else{
	        values.put(KEY_SEC_FOLDER_ENABLED, encrypt(enabled + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setCamSyncHandle (long handle){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_HANDLE + "= '" + encrypt(handle + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_HANDLE, encrypt(handle + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setSecondaryFolderHandle (long handle){
		log("setSecondaryFolderHandle: "+handle);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_HANDLE + "= '" + encrypt(handle + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_SEC_FOLDER_HANDLE, encrypt(handle + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setCamSyncLocalPath (String localPath){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_LOCAL_PATH + "= '" + encrypt(localPath + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(localPath + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setUriExternalSDCard (String uriExternalSDCard){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_URI_EXTERNAL_SD_CARD + "= '" + encrypt(uriExternalSDCard) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("KEY_URI_EXTERNAL_SD_CARD URI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_URI_EXTERNAL_SD_CARD, encrypt(uriExternalSDCard));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setCameraFolderExternalSDCard (boolean cameraFolderExternalSDCard){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD + "= '" + encrypt(cameraFolderExternalSDCard + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD, encrypt(cameraFolderExternalSDCard + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setPinLockType (String pinLockType){
		log("setPinLockType");
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_TYPE + "= '" + encrypt(pinLockType) + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PIN_LOCK_TYPE, encrypt(pinLockType));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setSecondaryFolderPath (String localPath){
		log("setSecondaryFolderPath: "+localPath);
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_FOLDER_LOCAL_PATH + "= '" + encrypt(localPath + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(localPath + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setCamSyncFileUpload (int fileUpload){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_FILE_UPLOAD + "= '" + encrypt(fileUpload + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(fileUpload + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}

	public void setAccountDetailsTimeStamp (){
		log("setAccountDetailsTimeStamp");
		long accountDetailsTimeStamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ACCOUNT_DETAILS_TIMESTAMP + "= '" + encrypt(accountDetailsTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_ACCOUNT_DETAILS_TIMESTAMP, encrypt(accountDetailsTimeStamp + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setPaymentMethodsTimeStamp (){
		log("setPaymentMethodsTimeStamp");
		long paymentMethodsTimeStamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_PAYMENT_METHODS_TIMESTAMP + "= '" + encrypt(paymentMethodsTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_PAYMENT_METHODS_TIMESTAMP, encrypt(paymentMethodsTimeStamp + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setPricingTimestamp (){
		log("setPricingTimestamp");
		long creditCardTimestamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_PRICING_TIMESTAMP + "= '" + encrypt(creditCardTimestamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_PRICING_TIMESTAMP, encrypt(creditCardTimestamp + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setExtendedAccountDetailsTimestamp (){
		log("setExtendedAccountDetailsTimestamp");
		long extendedAccountDetailsTimestamp = System.currentTimeMillis()/1000;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + "= '" + encrypt(extendedAccountDetailsTimestamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP, encrypt(extendedAccountDetailsTimestamp + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void resetExtendedAccountDetailsTimestamp (){
		log("resetExtendedAccountDetailsTimestamp");
		long extendedAccountDetailsTimestamp = -1;

		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTE_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP + "= '" + encrypt(extendedAccountDetailsTimestamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_ATTRIBUTE_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
			values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP, encrypt(extendedAccountDetailsTimestamp + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setCamSyncTimeStamp (long camSyncTimeStamp){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_TIMESTAMP + "= '" + encrypt(camSyncTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_TIMESTAMP, encrypt(camSyncTimeStamp + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setSecSyncTimeStamp (long secSyncTimeStamp){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_SEC_SYNC_TIMESTAMP + "= '" + encrypt(secSyncTimeStamp + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_SEC_SYNC_TIMESTAMP, encrypt(secSyncTimeStamp + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
		
	public void setPinLockEnabled (boolean pinLockEnabled){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_ENABLED + "= '" + encrypt(pinLockEnabled + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PIN_LOCK_ENABLED, encrypt(pinLockEnabled + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setPinLockCode (String pinLockCode){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_CODE + "= '" + encrypt(pinLockCode + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PIN_LOCK_CODE, encrypt(pinLockCode + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setStorageAskAlways (boolean storageAskAlways){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_ASK_ALWAYS + "= '" + encrypt(storageAskAlways + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_STORAGE_ASK_ALWAYS, encrypt(storageAskAlways + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setStorageAdvancedDevices (boolean storageAdvancedDevices){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_ADVANCED_DEVICES + "= '" + encrypt(storageAdvancedDevices + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_STORAGE_ADVANCED_DEVICES, encrypt(storageAdvancedDevices + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
	public void setStorageDownloadLocation (String storageDownloadLocation){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_DOWNLOAD_LOCATION + "= '" + encrypt(storageDownloadLocation + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
//			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(storageDownloadLocation + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
	}
	
//	public void setAttrOnline (boolean online){
//		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
//		ContentValues values = new ContentValues();
//		Cursor cursor = db.rawQuery(selectQuery, null);
//		if (cursor.moveToFirst()){
//			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ONLINE + "='" + encrypt(online + "") + "' WHERE " + KEY_ID + " ='1'";
//			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
//		}
//		else{
//			values.put(KEY_ATTR_ONLINE, encrypt(online + ""));
//			db.insert(TABLE_ATTRIBUTES, null, values);
//		}
//		cursor.close();
//	}
//
	public void setAttrAskSizeDownload (String askSizeDownload){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ASK_SIZE_DOWNLOAD + "='" + encrypt(askSizeDownload) + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(askSizeDownload));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}
	
	public void setAttrAskNoAppDownload (String askNoAppDownload){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ASK_NOAPP_DOWNLOAD + "='" + encrypt(askNoAppDownload) + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(askNoAppDownload));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}
	
	public void setAttrAttemps (int attemp){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_INTENTS + "='" + encrypt(Integer.toString(attemp) + "") + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_ATTR_INTENTS, encrypt(Integer.toString(attemp) + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setFileLoggerSDK (boolean fileLoggerSDK){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_FILE_LOGGER_SDK + "='" + encrypt(fileLoggerSDK + "") + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_FILE_LOGGER_SDK, encrypt(fileLoggerSDK + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setFileLoggerKarere (boolean fileLoggerKarere){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_FILE_LOGGER_KARERE + "='" + encrypt(fileLoggerKarere + "") + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_FILE_LOGGER_KARERE, encrypt(fileLoggerKarere + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}

	public void setUseHttpsOnly (boolean useHttpsOnly){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_USE_HTTPS_ONLY + "='" + encrypt(useHttpsOnly + "") + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_USE_HTTPS_ONLY, encrypt(useHttpsOnly + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}


	public String getUseHttpsOnly(){

		String selectQuery = "SELECT " + KEY_USE_HTTPS_ONLY + " FROM " + TABLE_ATTRIBUTES + " WHERE " + KEY_ID + " = '1'";
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){

			String useHttpsOnly = decrypt(cursor.getString(0));
			return useHttpsOnly;
		}
		cursor.close();

		return "false";
	}

	public void setInvalidateSdkCache(boolean invalidateSdkCache){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_INVALIDATE_SDK_CACHE + "='" + encrypt(invalidateSdkCache + "") + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_INVALIDATE_SDK_CACHE, encrypt(invalidateSdkCache + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}
	
	public void clearCredentials(){
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
	
//	public void clearOffline(){
//		log("clearOffline");
//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE);
//		onCreate(db);
//	}
	
	public void clearAttributes(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRIBUTES);
		onCreate(db);
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
	
	private static void log(String log) {
		Util.log("DatabaseHandler", log);
	}

}
