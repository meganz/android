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
	
	private static final int DATABASE_VERSION = 1; 
    private static final String DATABASE_NAME = "megapreferences"; 
    private static final String TABLE_PREFERENCES = "preferences";
    private static final String TABLE_CREDENTIALS = "credentials";
    private static final String KEY_ID = "id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PUBLIC_KEY= "publickey";
    private static final String KEY_PRIVATE_KEY = "privatekey";
    private static final String KEY_FIRST_LOGIN = "firstlogin";
    private static final String KEY_CAM_SYNC_ENABLED = "camsyncenabled";
    private static final String KEY_CAM_SYNC_HANDLE = "camsynchandle";
    private static final String KEY_WIFI = "wifi";
    private static final String KEY_CAM_SYNC_LOCAL_PATH = "camsynclocalpath";
    

	public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CREDENTIALS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CREDENTIALS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_EMAIL + " TEXT, " 
                + KEY_PUBLIC_KEY + " TEXT, " + KEY_PRIVATE_KEY + " TEXT" + ")";        
        db.execSQL(CREATE_CREDENTIALS_TABLE);
        
        String CREATE_PREFERENCES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PREFERENCES + "("
        		+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_FIRST_LOGIN + " BOOLEAN, "
        		+ KEY_CAM_SYNC_ENABLED + " BOOLEAN, " + KEY_CAM_SYNC_HANDLE + " TEXT, "
        		+ KEY_CAM_SYNC_LOCAL_PATH + " TEXT, " + KEY_WIFI + " BOOLEAN" + ")";
        db.execSQL(CREATE_PREFERENCES_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFERENCES); 
        onCreate(db);
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
	}
	
	private static byte[] getAesKey() {
		String key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC$G*(G($*GR*(#)*huio4h389$G";
		return Arrays.copyOfRange(key.getBytes(), 0, 32);
	}
	
	public void saveCredentials(UserCredentials userCredentials) {
        SQLiteDatabase db = this.getWritableDatabase(); 
        ContentValues values = new ContentValues();
        values.put(KEY_EMAIL, encrypt(userCredentials.getEmail()));
        values.put(KEY_PUBLIC_KEY, encrypt(userCredentials.getPublicKey()));
        values.put(KEY_PRIVATE_KEY, encrypt(userCredentials.getPrivateKey()));        
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
	}
	
	public UserCredentials getCredentials(){
		UserCredentials userCredentials = null;
		
		String selectQuery = "SELECT  * FROM " + TABLE_CREDENTIALS;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);		
		if (cursor.moveToFirst()) {
			int id = Integer.parseInt(cursor.getString(0));
			String email = decrypt(cursor.getString(1));
			String publicKey = decrypt(cursor.getString(2));
			String privateKey = decrypt(cursor.getString(3));
			userCredentials = new UserCredentials(email, privateKey, publicKey);
		}
		cursor.close();
        db.close();
        
        return userCredentials; 
	}
	
	public void setPreferences (Preferences prefs){
		SQLiteDatabase db = this.getWritableDatabase(); 
        ContentValues values = new ContentValues();
        values.put(KEY_FIRST_LOGIN, encrypt(prefs.getFirstTime()));
        values.put(KEY_WIFI, encrypt(prefs.getWifi()));
        values.put(KEY_CAM_SYNC_ENABLED, prefs.getCamSyncEnabled());
        values.put(KEY_CAM_SYNC_HANDLE, prefs.getCamSyncHandle());
        values.put(KEY_CAM_SYNC_LOCAL_PATH, prefs.getCamSyncLocalPath());
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
			prefs = new Preferences(firstTime, wifi, camSyncEnabled, camSyncHandle, camSyncLocalPath);
		}
		
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
        db.close();
	}
	
	public void setCamSyncWifi (boolean wifi){
		String selectQuery = "SELECT * FROM " + TABLE_PREFERENCES;
		SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()){
			String UPDATE_PREFERENCES_TABLE = "UPDATE " + TABLE_PREFERENCES + " SET " + KEY_WIFI + "= '" + encrypt(wifi + "") + "' WHERE " + KEY_ID + " = '1'";
			db.execSQL(UPDATE_PREFERENCES_TABLE);
			log("UPDATE_PREFERENCES_TABLE SYNC WIFI: " + UPDATE_PREFERENCES_TABLE);
		}
		else{
	        values.put(KEY_WIFI, encrypt(wifi + ""));
	        db.insert(TABLE_PREFERENCES, null, values);
		}
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
