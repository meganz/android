package mega.privacy.android.app;

import mega.privacy.android.app.lollipop.PinLockActivityLollipop;
import mega.privacy.android.app.utils.Util;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


public class PinUtil {
	
	static DatabaseHandler dbH = null;
	static MegaPreferences prefs = null;
	
	private static Context lastLocked = null;
	private static long lastPause;

	public static void resume(Context context) {
		log("resume");
//		dbH = new DatabaseHandler(context);
		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
		if (shouldLock(context)){
			lastLocked = context;
			log("lastLocked " + context);
			showLock(context);
		}
		else{
			log("lastLocked null");
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
							log("TIME: " + time + "__ lastPause: " + lastPause);
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
		log("showLock");
		Intent intent = new Intent(context, PinLockActivityLollipop.class);
		context.startActivity(intent);
	}
	
	// Update time since last check
	public static void update() {
		log("update");
		lastPause = System.currentTimeMillis();
		log("lastPause = " + lastPause);
		log("lastLocked = " + lastLocked);
	}
	
	// Pause handler
	public static void pause(Context context) {
		if (lastLocked != context) {
			update();
			log("contexts not equal..." + "context: " + context + "___lastLocked: " + lastLocked);
		}
		else{
			log("contexts equal..." + context);
		}
	}
	
	public static void log(String message) {
		Util.log("PinUtil", message);
	}
}
