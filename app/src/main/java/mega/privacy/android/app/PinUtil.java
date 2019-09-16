package mega.privacy.android.app;

import mega.privacy.android.app.lollipop.PinLockActivityLollipop;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

import android.content.Context;
import android.content.Intent;

public class PinUtil {
	
	static DatabaseHandler dbH = null;
	static MegaPreferences prefs = null;
	
	private static Context lastLocked = null;
	private static long lastPause;

	public static void resume(Context context) {
		LogUtil.logDebug("resume");
//		dbH = new DatabaseHandler(context);
		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
		if (shouldLock(context)){
			lastLocked = context;
			LogUtil.logDebug("lastLocked " + context);
			showLock(context);
		}
		else{
			LogUtil.logDebug("lastLocked null");
			lastLocked = null;
		}
	}
	
	// Check is app should lock
	private static boolean shouldLock(Context context) {
		if (prefs != null){
			if (prefs.getPinLockEnabled() != null){
				if (Boolean.parseBoolean(prefs.getPinLockEnabled())){
					if (prefs.getPinLockCode() != null){
						if (prefs.getPinLockCode().compareTo("") != 0){
							long time = System.currentTimeMillis();
							LogUtil.logDebug("TIME: " + time + " lastPause: " + lastPause);
							if ((time - lastPause) > (1 * 1000)) { //1 es el maximo de segundos hasta que hace que pasó la última acción para que se bloquee
								return true;
							}
						}
					}
				}
			}
		}
		
		return false;
	}
	
	// Display lock screen
	public static void showLock(Context context) {
		LogUtil.logDebug("showLock");
		Intent intent = new Intent(context, PinLockActivityLollipop.class);
		context.startActivity(intent);
	}
	
	// Update time since last check
	public static void update() {
		LogUtil.logDebug("update");
		lastPause = System.currentTimeMillis();
		LogUtil.logDebug("lastPause = " + lastPause);
		LogUtil.logDebug("lastLocked = " + lastLocked);
	}
	
	// Pause handler
	public static void pause(Context context) {
		if (lastLocked != context) {
			update();
			LogUtil.logDebug("contexts not equal..." + "context: " + context + "___lastLocked: " + lastLocked);
		}
		else{
			LogUtil.logDebug("contexts equal..." + context);
		}
	}
}
