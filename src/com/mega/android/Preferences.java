package com.mega.android;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Base64;

public class Preferences extends PreferenceActivity{
	
	public static String FILE = "prefs_main.xml";
	// Preferences keys
	public static String KEY_EMAIL = "email";
	public static String KEY_PASSWORD2 = "password";
	public static String KEY_PUBLIC_KEY = "public_key";
	public static String KEY_PRIVATE_KEY = "private_key";
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
	}
	
	/*
	 * Get static preference object
	 */
	public static SharedPreferences getPreferences(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(FILE, 0);
		return prefs;
	}
	
	/*
	 * Get user credentials or null if not available
	 */
	synchronized public static UserCredentials getCredentials(Context context){
		if (context == null){
			return null;
		}
		
		SharedPreferences prefs = getPreferences(context);
		
		String email = decrypt(prefs.getString(KEY_EMAIL, null));
		if(email == null) return null;
		
		String publicKey = decrypt(prefs.getString(KEY_PUBLIC_KEY, null));
		if(publicKey == null) return null;
		
		String privateKey = decrypt(prefs.getString(KEY_PRIVATE_KEY, null));
		if (privateKey == null) return null;
		
		return new UserCredentials(email, privateKey, publicKey);
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
	
	/*
	 * Remove user credentials
	 */
	public static void clearCredentials(Context context) {
		SharedPreferences prefs = getPreferences(context);
		Editor editor = prefs.edit();
		editor.clear();
		editor.commit();
	}
	
	private static byte[] getAesKey() {
		String key = Settings.Secure.ANDROID_ID
				+ "fkvn8 w4y*(NC$G*(G($*GR*(#)*huio4h389$G";
		return Arrays.copyOfRange(key.getBytes(), 0, 32);
	}
	
	private static void log(String log) {
		Util.log("Preferences", log);
	}

}
