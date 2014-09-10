package com.mega.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.util.Base64;

public class DatabaseHandler extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 6; 
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
    private static final String KEY_CAM_SYNC_HANDLE = "camsynchandle";
    private static final String KEY_CAM_SYNC_WIFI = "wifi";
    private static final String KEY_CAM_SYNC_LOCAL_PATH = "camsynclocalpath";
    private static final String KEY_CAM_SYNC_FILE_UPLOAD = "fileUpload";
    private static final String KEY_PIN_LOCK_ENABLED = "pinlockenabled";
    private static final String KEY_PIN_LOCK_CODE = "pinlockcode";
    private static final String KEY_STORAGE_ASK_ALWAYS = "storageaskalways";
    private static final String KEY_STORAGE_DOWNLOAD_LOCATION = "storagedownloadlocation";
    private static final String KEY_ATTR_ONLINE = "online";
    private static final String KEY_OFF_HANDLE = "handle";
    private static final String KEY_OFF_PATH = "path";
    private static final String KEY_OFF_NAME = "name";
    private static final String KEY_OFF_PARENT = "parentId";
    private static final String KEY_OFF_TYPE = "type";

	public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
        String CREATE_OFFLINE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_OFFLINE + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_OFF_HANDLE + " TEXT," + KEY_OFF_PATH + " TEXT," + KEY_OFF_NAME + " TEXT," + KEY_OFF_PARENT + " INTEGER," + KEY_OFF_TYPE + " INTEGER"+", FOREIGN KEY (" + KEY_OFF_PARENT + ") REFERENCES "+ TABLE_OFFLINE +" ("+ KEY_ID +"))";
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
        		KEY_STORAGE_DOWNLOAD_LOCATION + " TEXT" + ")";
        db.execSQL(CREATE_PREFERENCES_TABLE);
        
        String CREATE_ATTRIBUTES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTRIBUTES + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_ATTR_ONLINE + " TEXT" + ")";
        db.execSQL(CREATE_ATTRIBUTES_TABLE);
        

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		UserCredentials userCredentials = null;
		
		String selectQuery = "SELECT  * FROM " + TABLE_CREDENTIALS;
		Cursor cursor = db.rawQuery(selectQuery, null);		
		if (cursor.moveToFirst()) {
			int id = Integer.parseInt(cursor.getString(0));
			String email = decrypt(cursor.getString(1));
			String session = decrypt(cursor.getString(2));
			userCredentials = new UserCredentials(email, session);
		}
		cursor.close();
        
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES); 
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRIBUTES);
        onCreate(db);
        
        ContentValues values = new ContentValues();
        values.put(KEY_EMAIL, encrypt(userCredentials.getEmail()));
        values.put(KEY_SESSION, encrypt(userCredentials.getSession()));
        db.insert(TABLE_CREDENTIALS, null, values);
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
        SQLiteDatabase db = this.getWritableDatabase(); 
        ContentValues values = new ContentValues();
        values.put(KEY_EMAIL, encrypt(userCredentials.getEmail()));
        values.put(KEY_SESSION, encrypt(userCredentials.getSession()));
        db.insert(TABLE_CREDENTIALS, null, values);
        db.close(); 
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
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);		
		if (cursor.moveToFirst()) {
			int id = Integer.parseInt(cursor.getString(0));
			String email = decrypt(cursor.getString(1));
			String session = decrypt(cursor.getString(2));
			userCredentials = new UserCredentials(email, session);
		}
		cursor.close();
        db.close();
        
        return userCredentials; 
	}
	
	public void setPreferences (MegaPreferences prefs){
		SQLiteDatabase db = this.getWritableDatabase(); 
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
        db.insert(TABLE_PREFERENCES, null, values);
        db.close();
	}
	
	public MegaPreferences getPreferences(){
		MegaPreferences prefs = null;
		
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
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
			prefs = new MegaPreferences(firstTime, wifi, camSyncEnabled, camSyncHandle, camSyncLocalPath, fileUpload, pinLockEnabled, pinLockCode, askAlways, downloadLocation);
		}
		cursor.close();
        db.close();
		
		return prefs;
	}
	
	public void setAttributes (MegaAttributes attr){
		SQLiteDatabase db = this.getWritableDatabase(); 
        ContentValues values = new ContentValues();
        values.put(KEY_ATTR_ONLINE, encrypt(attr.getOnline()));
        db.insert(TABLE_ATTRIBUTES, null, values);
        db.close();
	}
	
	public MegaAttributes getAttributes(){
		MegaAttributes attr = null;
		
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			int id = Integer.parseInt(cursor.getString(0));
			String online = decrypt(cursor.getString(1));
			attr = new MegaAttributes(online);
		}
		cursor.close();
		db.close();
		
		return attr;
	}
	
	public long setOfflineFile (MegaOffline offline){
		
		SQLiteDatabase db = this.getWritableDatabase(); 
        ContentValues values = new ContentValues();
        String nullColumnHack = null;
        
        values.put(KEY_OFF_HANDLE, offline.getHandle());
        values.put(KEY_OFF_PATH, offline.getPath());
        values.put(KEY_OFF_NAME, offline.getName());
        values.put(KEY_OFF_PARENT, offline.getparentId());
        values.put(KEY_OFF_TYPE, offline.getType());
        
        long ret = db.insert(TABLE_OFFLINE, nullColumnHack, values);
        db.close();
        
        return ret;
		
	}
		
	public ArrayList<MegaOffline> getOfflineFiles (){
		
		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();

		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			do{
				
				int id = Integer.parseInt(cursor.getString(0));
				String handle = decrypt(cursor.getString(1));
				String path = decrypt(cursor.getString(2));
				String name = decrypt(cursor.getString(3));
				int parent = cursor.getInt(4);
				String type = decrypt(cursor.getString(5));			
				MegaOffline offline = new MegaOffline(id,handle, path, name, parent, type);
				listOffline.add(offline);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();

		return listOffline;
	}

	public boolean exists(long handle){
		
				
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + handle + "'";
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		if (!cursor.equals(null))
	        return cursor.moveToFirst();
	    
		return false; 		 
	}
	
	public MegaOffline findByHandle(long handle){

		MegaOffline offline = null;
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + handle + "'";

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);	


		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;

				_id = Integer.parseInt(cursor.getString(0));
				_handle = cursor.getString(1);
				_path = cursor.getString(2);
				_name = cursor.getString(3);
				_parent = cursor.getInt(4);
				_type = cursor.getString(5);
				offline = new MegaOffline(_id,_handle, _path, _name, _parent, _type);
			}
		}
		return offline; 		 
	}
	
	public MegaOffline findByHandle(String handle){

		MegaOffline offline = null;
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_HANDLE + " = '" + handle + "'";

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);	


		if (!cursor.equals(null)){
			if (cursor.moveToFirst()){		

				int _id = -1;
				int _parent = -1;
				String _handle = null;
				String _path = null;
				String _name = null;
				String _type = null;

				_id = Integer.parseInt(cursor.getString(0));
				_handle = cursor.getString(1);
				_path = cursor.getString(2);
				_name = cursor.getString(3);
				_parent = cursor.getInt(4);
				_type = cursor.getString(5);
				offline = new MegaOffline(_id,_handle, _path, _name, _parent, _type);
			}
		}
		return offline; 		 
	}
	
	public ArrayList<MegaOffline> findByParentId(int parentId){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PARENT + " = '" + parentId + "'";

		SQLiteDatabase db = this.getWritableDatabase();
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
	
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					listOffline.add(new MegaOffline(_id,_handle, _path, _name, _parent, _type));
				} while (cursor.moveToNext());
			}
		}
		return listOffline; 		 
	}
	
	public MegaOffline findById(int id){		
		
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_ID + " = '" + id + "'";
		MegaOffline mOffline = null;
		SQLiteDatabase db = this.getWritableDatabase();
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
	
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					mOffline = new MegaOffline (_id,_handle, _path, _name, _parent, _type);
					
				} while (cursor.moveToNext());
			}
		}
		return mOffline; 		 
	}
	
	public boolean removeById(int id){	

		SQLiteDatabase db = this.getWritableDatabase();
		return db.delete(TABLE_OFFLINE, KEY_ID + "="+id, null) > 0;		
		
	}	
	
	public ArrayList<MegaOffline> findByPath(String path){

		ArrayList<MegaOffline> listOffline = new ArrayList<MegaOffline>();
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + path + "'";

		SQLiteDatabase db = this.getWritableDatabase();
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
	
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					listOffline.add(new MegaOffline(_id,_handle, _path, _name, _parent, _type));
				} while (cursor.moveToNext());
			}
		}
		return listOffline; 		 
	}
	
	public MegaOffline findbyPathAndName(String path, String name){
		
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + path + "'" + "AND " + KEY_OFF_NAME + " = '" + name + "'"  ;
		
		MegaOffline mOffline = null;
		SQLiteDatabase db = this.getWritableDatabase();
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
	
					_id = Integer.parseInt(cursor.getString(0));
					_handle = cursor.getString(1);
					_path = cursor.getString(2);
					_name = cursor.getString(3);
					_parent = cursor.getInt(4);
					_type = cursor.getString(5);
					mOffline = new MegaOffline (_id,_handle, _path, _name, _parent, _type);
					
				} while (cursor.moveToNext());
			}
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
		
		
		//Get the foreign key of the node 
		String selectQuery = "SELECT * FROM " + TABLE_OFFLINE + " WHERE " + KEY_OFF_PATH + " = '" + path + "'" + "AND" + KEY_OFF_NAME + " = '" + name + "'"  ;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		if (cursor.moveToFirst()){			
				
			_id = Integer.parseInt(cursor.getString(0));
			_handle = decrypt(cursor.getString(1));
			_path = decrypt(cursor.getString(2));
			_name = decrypt(cursor.getString(3));
			_parent = cursor.getInt(4);
			_type = decrypt(cursor.getString(5));			
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
					MegaOffline offline = new MegaOffline(_handle, _path, _name, _parent, _type);
					listOffline.add(offline);
				} while (cursor.moveToNext());
			}
			cursor.close();
			db.close();			
		}		
		
		return listOffline; 		
	}

	public void deleteOfflineFile (MegaOffline mOff) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    
	    db.delete(TABLE_OFFLINE, KEY_OFF_HANDLE + " = ?",
	            new String[] { String.valueOf(mOff.getHandle()) });
	            
	    db.close();
	}
	
	public void setFirstTime (boolean firstTime){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_FIRST_LOGIN + "= '" + encrypt(firstTime + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_FIRST_LOGIN, encrypt(firstTime + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setCamSyncWifi (boolean wifi){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_WIFI + "= '" + encrypt(wifi + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_WIFI, encrypt(wifi + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setCamSyncEnabled (boolean enabled){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_ENABLED + "= '" + encrypt(enabled + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_ENABLED, encrypt(enabled + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setCamSyncHandle (long handle){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_HANDLE + "= '" + encrypt(handle + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_HANDLE, encrypt(handle + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setCamSyncLocalPath (String localPath){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_LOCAL_PATH + "= '" + encrypt(localPath + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(localPath + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setCamSyncFileUpload (int fileUpload){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_CAM_SYNC_FILE_UPLOAD + "= '" + encrypt(fileUpload + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(fileUpload + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setPinLockEnabled (boolean pinLockEnabled){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_ENABLED + "= '" + encrypt(pinLockEnabled + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PIN_LOCK_ENABLED, encrypt(pinLockEnabled + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setPinLockCode (String pinLockCode){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_PIN_LOCK_CODE + "= '" + encrypt(pinLockCode + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_PIN_LOCK_CODE, encrypt(pinLockCode + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setStorageAskAlways (boolean storageAskAlways){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_ASK_ALWAYS + "= '" + encrypt(storageAskAlways + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_STORAGE_ASK_ALWAYS, encrypt(storageAskAlways + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setStorageDownloadLocation (String storageDownloadLocation){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_STORAGE_DOWNLOAD_LOCATION + "= '" + encrypt(storageDownloadLocation + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC ENABLED: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(storageDownloadLocation + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
		cursor.close();
        db.close();
	}
	
	public void setAttrOnline (boolean online){
		String selectQuery = "SELECT * FROM " + TABLE_ATTRIBUTES;
		SQLiteDatabase db = this.getWritableDatabase();
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
		db.close();
	}
	
	public void clearCredentials(){
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);   
        onCreate(db);
	}
	
	public void clearPreferences(){
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES);   
        onCreate(db);
	}
	
	private static void log(String log) {
		Util.log("DatabaseHandler", log);
	}

}
