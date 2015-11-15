package mega.privacy.android.app;

import java.util.Arrays;

import mega.privacy.android.app.utils.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;
import android.util.Base64;

public class OldPreferences {
	
	public static String FILE = "prefs_main.xml";
	public static String KEY_EMAIL = "email";
	public static String KEY_PUBLIC_KEY = "public_key";
	public static String KEY_PRIVATE_KEY = "private_key";
	
	/*
	 * Get user credentials or null if not available
	 */
	synchronized public static OldUserCredentials getOldCredentials(Context context) {
		if(context == null) return null;
		SharedPreferences prefs = getPreferences(context);
		
		String email = decrypt(prefs.getString(KEY_EMAIL, null));
		if(email == null) return null;
		
		String public_key = decrypt(prefs.getString(KEY_PUBLIC_KEY, null));
		if(public_key == null) return null;
		
		String private_key = decrypt(prefs.getString(KEY_PRIVATE_KEY, null));
		if (private_key == null) return null;
		
		return new OldUserCredentials(email, private_key, public_key);
	}
	
	/*
	 * Get static preference object
	 */
	public static SharedPreferences getPreferences(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(FILE, 0);
		return prefs;
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
	
	private static byte[] getAesKey() {
		String key = Settings.Secure.ANDROID_ID
				+ "fkvn8 w4y*(NC$G*(G($*GR*(#)*huio4h389$G";
		return Arrays.copyOfRange(key.getBytes(), 0, 32);
	}
	
	public static void clearCredentials(Context context) {
		SharedPreferences prefs = getPreferences(context);
		Editor editor = prefs.edit();
		editor.clear();
		editor.commit();
	}
	
	public static void log(String message) {
		Util.log("OldPreferences", message);
	}
}
