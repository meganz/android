package mega.privacy.android.app;

import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity;

import android.content.Context;
import android.content.Intent;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class PinUtil {
    static DatabaseHandler dbH = null;
    static MegaPreferences prefs = null;

    private static Context lastLocked = null;
    private static long lastPause;

    public static void resume(Context context) {
        logDebug("resume");

        dbH = DatabaseHandler.getDbHandler(context);
        prefs = dbH.getPreferences();

        if (shouldLock()) {
            lastLocked = context;
            logDebug("lastLocked " + context);
            showLock(context);
        } else {
            logDebug("lastLocked null");
            lastLocked = null;
        }
    }

    // Check is app should lock
    private static boolean shouldLock() {
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
