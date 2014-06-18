package com.mega.android;

import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.util.Base64;

public class DatabaseHandler extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 5; 
    private static final String DATABASE_NAME = "megapreferences"; 
    private static final String TABLE_PREFERENCES = "preferences";
    private static final String TABLE_CREDENTIALS = "credentials";
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
    

	public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
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
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		UserCredentials userCredentials = null;
//		
//		String selectQuery = "SELECT  * FROM " + TABLE_CREDENTIALS;
//		Cursor cursor = db.rawQuery(selectQuery, null);		
//		if (cursor.moveToFirst()) {
//			int id = Integer.parseInt(cursor.getString(0));
//			String email = decrypt(cursor.getString(1));
//			String session = decrypt(cursor.getString(2));
//			userCredentials = new UserCredentials(email, session);
//		}
//		cursor.close();
        
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES); 
        onCreate(db);
        
//        ContentValues values = new ContentValues();
//        values.put(KEY_EMAIL, encrypt(userCredentials.getEmail()));
//        values.put(KEY_SESSION, encrypt(userCredentials.getSession()));
//        db.insert(TABLE_CREDENTIALS, null, values);
	} 
	
	public static String encrypt(String original) {
//		if (original == null) {
//			return null;
//		}
//		try {
//			byte[] encrypted = Util.aes_encrypt(getAesKey(),original.getBytes());
//			return Base64.encodeToString(encrypted, Base64.DEFAULT);
//		} catch (Exception e) {
//			log("ee");
//			e.printStackTrace();
//			return null;
//		}
		return original;
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
//		if (encodedString == null) {
//			return null;
//		}
//		try {
//			byte[] encoded = Base64.decode(encodedString, Base64.DEFAULT);
//			byte[] original = Util.aes_decrypt(getAesKey(), encoded);
//			return new String(original);
//		} catch (Exception e) {
//			log("de");
//			return null;
//		}
		return encodedString;
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
	
	public void setPreferences (Preferences prefs){
		SQLiteDatabase db = this.getWritableDatabase(); 
        ContentValues values = new ContentValues();
        values.put(KEY_FIRST_LOGIN, encrypt(prefs.getFirstTime()));
        values.put(KEY_CAM_SYNC_WIFI, encrypt(prefs.getCamSyncWifi()));
        values.put(KEY_CAM_SYNC_ENABLED, prefs.getCamSyncEnabled());
        values.put(KEY_CAM_SYNC_HANDLE, prefs.getCamSyncHandle());
        values.put(KEY_CAM_SYNC_LOCAL_PATH, prefs.getCamSyncLocalPath());
        values.put(KEY_CAM_SYNC_FILE_UPLOAD, prefs.getCamSyncFileUpload());
        values.put(KEY_PIN_LOCK_ENABLED, prefs.getPinLockEnabled());
        values.put(KEY_PIN_LOCK_CODE, prefs.getPinLockCode());
        values.put(KEY_STORAGE_ASK_ALWAYS, prefs.getStorageAskAlways());
        values.put(KEY_STORAGE_DOWNLOAD_LOCATION, prefs.getStorageDownloadLocation());
        db.insert(TABLE_PREFERENCES, null, values);
        db.close();
	}
	
	public Preferences getPreferences(){
		Preferences prefs = null;
		
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
			prefs = new Preferences(firstTime, wifi, camSyncEnabled, camSyncHandle, camSyncLocalPath, fileUpload, pinLockEnabled, pinLockCode, askAlways, downloadLocation);
		}
		cursor.close();
        db.close();
		
		return prefs;
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
	
	public void clearCredentials(){
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);   
        onCreate(db);
	}
	
	private static void log(String log) {
		Util.log("DatabaseHandler", log);
	}

}
