package mega.privacy.android.app;

import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity;

import android.content.Context;
import android.content.Intent;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class PinUtil {
	public static final int REQUIRE_PASSCODE_IMMEDIATE = 0;
	public static final int REQUIRE_PASSCODE_AFTER_5S = 5 * 1000;
	public static final int REQUIRE_PASSCODE_AFTER_10S = 10 * 1000;
	public static final int REQUIRE_PASSCODE_AFTER_30S = 30 * 1000;
	public static final int REQUIRE_PASSCODE_AFTER_1M = 60 * 1000;
	public static final int REQUIRE_PASSCODE_AFTER_2M = 60 * 2 * 1000;
	public static final int REQUIRE_PASSCODE_AFTER_5M = 60 * 5 * 1000;

    static DatabaseHandler dbH = null;
    static MegaPreferences prefs = null;

    private static Context lastLocked = null;
    private static long lastPause;

    public static void resume(Context context) {
        logDebug("resume");

        dbH = DatabaseHandler.getDbHandler(context);
        prefs = dbH.getPreferences();

        if (shouldLock(context)) {
            lastLocked = context;
            logDebug("lastLocked " + context);
            showLock(context);
        } else {
            logDebug("lastLocked null");
            lastLocked = null;
        }
    }

    // Check is app should lock
    private static boolean shouldLock(Context context) {
        if (prefs != null && prefs.getPasscodeLockEnabled() != null
                && Boolean.parseBoolean(prefs.getPasscodeLockEnabled())
                && !isTextEmpty(prefs.getPasscodeLockCode())) {
            long time = System.currentTimeMillis();
            logDebug("TIME: " + time + " lastPause: " + lastPause);

            //1 es el maximo de segundos hasta que hace que pasó la última acción para que se bloquee
            return time - lastPause > 1 * 1000;
        }

        return false;
    }

    // Display lock screen
    public static void showLock(Context context) {
        logDebug("showLock");
        Intent intent = new Intent(context, PasscodeLockActivity.class);
        context.startActivity(intent);
    }

    // Update time since last check
    public static void update() {
        logDebug("update");
        lastPause = System.currentTimeMillis();
        logDebug("lastPause = " + lastPause);
        logDebug("lastLocked = " + lastLocked);
    }

    // Pause handler
    public static void pause(Context context) {
        if (lastLocked != context) {
            update();
            logDebug("contexts not equal..." + "context: " + context + "___lastLocked: " + lastLocked);
        } else {
            logDebug("contexts equal..." + context);
        }
    }
}
