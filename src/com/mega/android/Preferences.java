package com.mega.android;

public class Preferences{
	
	String firstTime = "";
	String wifi = "";
	String camSyncEnabled = "";
	String camSyncHandle = "";
	String camSyncLocalPath = "";
	
	Preferences(String firstTime, String wifi, String camSyncEnabled, String camSyncHandle, String camSyncLocalPath){
		this.firstTime = firstTime;
		this.wifi = wifi;
		this.camSyncEnabled = camSyncEnabled;
		this.camSyncHandle = camSyncHandle;
		this.camSyncLocalPath = camSyncLocalPath;
	}
	
	public String getFirstTime (){
		return firstTime;
	}
	
	public void setFirstTime(String firstTime){
		this.firstTime = firstTime;
	}
	
	public String getCamSyncEnabled(){
		return camSyncEnabled;
	}
	
	public void setCamSyncEnabled(String camSyncEnabled){
		this.camSyncEnabled = camSyncEnabled;
	}
	
	public String getCamSyncHandle(){
		return camSyncHandle;
	}
	
	public void setCamSyncHandle(String camSyncHandle){
		this.camSyncHandle = camSyncHandle;
	}
	
	public String getCamSyncLocalPath(){
		return camSyncLocalPath;
	}
	
	public void setCamSyncLocalPath(String camSyncLocalPath){
		this.camSyncLocalPath = camSyncLocalPath;
	}
	
	public String getWifi (){
		return wifi;
	}
	
	public void setWifi(String wifi){
		this.wifi = wifi;
	}
}

//import java.util.Arrays;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.Editor;
//import android.os.Bundle;
//import android.preference.PreferenceActivity;
//import android.provider.Settings;
//import android.util.Base64;
//
//public class Preferences extends PreferenceActivity{
//	
//	public static String FILE = "prefs_main.xml";
//	// Preferences keys
//	public static String KEY_EMAIL = "email";
//	public static String KEY_PASSWORD2 = "password";
//	public static String KEY_PUBLIC_KEY = "public_key";
//	public static String KEY_PRIVATE_KEY = "private_key";
//	
//	@Override
//	public void onCreate(Bundle savedInstanceState){
//		super.onCreate(savedInstanceState);
//		
//	}
//	
//	/*
//	 * Get static preference object
//	 */
//	public static SharedPreferences getPreferences(Context context) {
//		SharedPreferences prefs = context.getSharedPreferences(FILE, 0);
//		return prefs;
//	}
//	
//	/*
//	 * Get user credentials or null if not available
//	 */
//	synchronized public static UserCredentials getCredentials(Context context){
//		if (context == null){
//			return null;
//		}
//		
//		SharedPreferences prefs = getPreferences(context);
//		
//		String email = decrypt(prefs.getString(KEY_EMAIL, null));
//		if(email == null) return null;
//		
//		String publicKey = decrypt(prefs.getString(KEY_PUBLIC_KEY, null));
//		if(publicKey == null) return null;
//		
//		String privateKey = decrypt(prefs.getString(KEY_PRIVATE_KEY, null));
//		if (privateKey == null) return null;
//		
//		return new UserCredentials(email, privateKey, publicKey);
//	}
//	
//	public static String encrypt(String original) {
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
//	}
//	
//	public static String decrypt(String encodedString) {
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
//	}
//	
//	/*
//	 * Save user credentials
//	 */
//	public static void saveCredentials(Context context,
//			UserCredentials credentials) {
//		if(context == null) return;
//		SharedPreferences prefs = getPreferences(context);
//		Editor editor = prefs.edit();
//		editor.putString(Preferences.KEY_EMAIL, encrypt(credentials.getEmail()));
//		editor.putString(Preferences.KEY_PUBLIC_KEY,
//				encrypt(credentials.getPublicKey()));
//		editor.putString(Preferences.KEY_PRIVATE_KEY,
//				encrypt(credentials.getPrivateKey()));
//		editor.commit();
//	}
//	
//	/*
//	 * Remove user credentials
//	 */
//	public static void clearCredentials(Context context) {
//		SharedPreferences prefs = getPreferences(context);
//		Editor editor = prefs.edit();
//		editor.clear();
//		editor.commit();
//	}
//	
//	private static byte[] getAesKey() {
//		String key = Settings.Secure.ANDROID_ID
//				+ "fkvn8 w4y*(NC$G*(G($*GR*(#)*huio4h389$G";
//		return Arrays.copyOfRange(key.getBytes(), 0, 32);
//	}
//	
//	private static void log(String log) {
//		Util.log("Preferences", log);
//	}
//
//}
