package nz.mega.android;

import java.util.ArrayList;
import java.util.Arrays;

import nz.mega.android.utils.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.util.Base64;


public class DatabaseHandler extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 14; 
    private static final String DATABASE_NAME = "megapreferences"; 
    private static final String TABLE_PREFERENCES = "preferences";
    private static final String TABLE_CREDENTIALS = "credentials";
    private static final String TABLE_ATTRIBUTES = "attributes";
    private static final String TABLE_OFFLINE = "offline";
    private static final String KEY_ID = "id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SESSION= "session";
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
    private static final String KEY_PIN_LOCK_CODE = "pinlockcode";
    private static final String KEY_STORAGE_ASK_ALWAYS = "storageaskalways";
    private static final String KEY_STORAGE_DOWNLOAD_LOCATION = "storagedownloadlocation";
    private static final String KEY_LAST_UPLOAD_FOLDER = "lastuploadfolder";
    private static final String KEY_LAST_CLOUD_FOLDER_HANDLE = "lastcloudfolder";
    private static final String KEY_ATTR_ONLINE = "online";
    private static final String KEY_OFF_HANDLE = "handle";
    private static final String KEY_OFF_PATH = "path";
    private static final String KEY_OFF_NAME = "name";
    private static final String KEY_OFF_PARENT = "parentId";
    private static final String KEY_OFF_TYPE = "type";
    private static final String KEY_OFF_INCOMING = "incoming";
    private static final String KEY_OFF_HANDLE_INCOMING = "incomingHandle";
    private static final String KEY_SEC_SYNC_TIMESTAMP = "secondarySyncTimeStamp";
    private static final String KEY_STORAGE_ADVANCED_DEVICES = "storageadvanceddevices";

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
                + KEY_SESSION + " TEXT" + ")";        
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
        		KEY_STORAGE_ADVANCED_DEVICES+ "	BOOLEAN"+")";
        
        db.execSQL(CREATE_PREFERENCES_TABLE);
        
        String CREATE_ATTRIBUTES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTRIBUTES + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_ATTR_ONLINE + " TEXT" + ")";
        db.execSQL(CREATE_ATTRIBUTES_TABLE);
  
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
	} 
	
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
        db.insert(TABLE_CREDENTIALS, null, values);
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
				userCredentials = new UserCredentials(email, session);
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
			
			prefs = new MegaPreferences(firstTime, wifi, camSyncEnabled, camSyncHandle, camSyncLocalPath, fileUpload, camSyncTimeStamp, pinLockEnabled, 
					pinLockCode, askAlways, downloadLocation, camSyncCharging, lastFolderUpload, lastFolderCloud, secondaryFolderEnabled, secondaryPath, secondaryHandle, 
					secSyncTimeStamp, keepFileNames, storageAdvancedDevices);
		}
		cursor.close();
		
		return prefs;
	}
	
	public void setAttributes (MegaAttributes attr){
        ContentValues values = new ContentValues();
        values.put(KEY_ATTR_ONLINE, encrypt(attr.getOnline()));
        db.insert(TABLE_ATTRIBUTES, null, values);
	}
	
	public MegaAttributes getAttributes(){
		MegaAttributes attr = null;
		
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			int id = Integer.parseInt(cursor.getString(0));
			String online = decrypt(cursor.getString(1));
			attr = new MegaAttributes(online);
		}
		cursor.close();
		
		return attr;
	}
	
	public long setOfflineFile (MegaOffline offline){
		
        ContentValues values = new ContentValues();
        
        MegaOffline checkInsert = null;
        checkInsert=findByHandle(offline.getHandle());              
        
        if(checkInsert==null){
        	String nullColumnHack = null;        	
            
            values.put(KEY_OFF_HANDLE, offline.getHandle());
            values.put(KEY_OFF_PATH, offline.getPath());
            values.put(KEY_OFF_NAME, offline.getName());
            values.put(KEY_OFF_PARENT, offline.getparentId());
            values.put(KEY_OFF_TYPE, offline.getType());
            values.put(KEY_OFF_INCOMING, offline.isIncoming());
            values.put(KEY_OFF_HANDLE_INCOMING, offline.getHandleIncoming());
            
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
				boolean incoming = (cursor.getInt(6) == 1);
				String handleIncoming = decrypt(cursor.getString(7));
				MegaOffline offline = new MegaOffline(id,handle, path, name, parent, type, incoming, handleIncoming);
				listOffline.add(offline);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return listOffline;
	}

	public boolean exists(long handle){
				
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + handle + "'";
		
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

		MegaOffline offline = null;
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + handle + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);	


		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;
				boolean _incoming = false;
				String _handleIncoming = null;

				_id = Integer.parseInt(cursor.getString(0));
				_handle = cursor.getString(1);
				_path = cursor.getString(2);
				_name = cursor.getString(3);
				_parent = cursor.getInt(4);
				_type = cursor.getString(5);
				_incoming = (cursor.getInt(6) == 1);
				_handleIncoming = cursor.getString(7);
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
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + handle + "'";

		Cursor cursor = db.rawQuery(selectQuery, null);	

		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;
				boolean _incoming = false;
				String _handleIncoming = null;
				
				_id = Integer.parseInt(cursor.getString(0));
				_handle = cursor.getString(1);
				_path = cursor.getString(2);
				_name = cursor.getString(3);
				_parent = cursor.getInt(4);
				_type = cursor.getString(5);
				_incoming = (cursor.getInt(6) == 1);
				_handleIncoming = cursor.getString(7);
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
					boolean _incoming = false;
					String _handleIncoming = null;
					
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					_incoming = (cursor.getInt(6) == 1);
					_handleIncoming = cursor.getString(7);
					
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
					boolean _incoming = false;
					String _handleIncoming = null;
					
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					_incoming = (cursor.getInt(6) == 1);
					_handleIncoming = cursor.getString(7);
					
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
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + path + "'";

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
					boolean _incoming = false;
					String _handleIncoming = null;
	
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					_incoming = (cursor.getInt(6) == 1);
					_handleIncoming = cursor.getString(7);
					
					listOffline.add(new MegaOffline(_id,_handle, _path, _name, _parent, _type, _incoming, _handleIncoming));
				} while (cursor.moveToNext());
			}
		}
		cursor.close();
		return listOffline; 		 
	}
	
	public MegaOffline findbyPathAndName(String path, String name){
		
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + path + "'" + "AND " + KEY_OFF_NAME + " = '" + name + "'"  ;
		
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
					boolean _incoming = false;
					String _handleIncoming = null;
					
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					_incoming = (cursor.getInt(6) == 1);
					_handleIncoming = cursor.getString(7);
					
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
		boolean _incoming = false;
		String _handleIncoming = null;
		
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + path + "'" + "AND" + KEY_OFF_NAME + " = '" + name + "'"  ;
		
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		if (cursor.moveToFirst()){			
				
			_id = Integer.parseInt(cursor.getString(0));
			_handle = decrypt(cursor.getString(1));
			_path = decrypt(cursor.getString(2));
			_name = decrypt(cursor.getString(3));
			_parent = cursor.getInt(4);
			_type = decrypt(cursor.getString(5));
			_incoming = (cursor.getInt(6) == 1);
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
					_incoming = (cursor.getInt(6) == 1);
					_handleIncoming = cursor.getString(7);
					
					MegaOffline offline = new MegaOffline(_handle, _path, _name, _parent, _type, _incoming, _handleIncoming);
					listOffline.add(offline);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}		
		
		return listOffline; 		
	}

	public void deleteOfflineFile (MegaOffline mOff) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    
	    db.delete(TABLE_OFFLINE, KEY_OFF_HANDLE + " = ?",
	            new String[] { String.valueOf(mOff.getHandle()) });
	            
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
		log("setCamSyncEnabled");
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
	
	public void setAttrOnline (boolean online){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_ATTRIBUTES_TABLE = "UPDATE " + TABLE_ATTRIBUTES + " SET " + KEY_ATTR_ONLINE + "='" + encrypt(online + "") + "' WHERE " + KEY_ID + " ='1'";
			db.execSQL(UPDATE_ATTRIBUTES_TABLE);
			log("UPDATE_ATTRIBUTES_TABLE : " + UPDATE_ATTRIBUTES_TABLE);
		}
		else{
			values.put(KEY_ATTR_ONLINE, encrypt(online + ""));
			db.insert(TABLE_ATTRIBUTES, null, values);
		}
		cursor.close();
	}
	
	public void clearCredentials(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);   
        onCreate(db);
	}
	
	public void clearPreferences(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);   
        onCreate(db);
	}
	
	public void clearOffline(){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE);
		onCreate(db);
	}
	
	private static void log(String log) {
		Util.log("DatabaseHandler", log);
	}

}
