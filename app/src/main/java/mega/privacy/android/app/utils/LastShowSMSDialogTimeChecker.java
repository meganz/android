package mega.privacy.android.app.utils;


import android.content.Context;
import android.content.SharedPreferences;

public class LastShowSMSDialogTimeChecker {

    public static final int WEEK = 7 * 24 * 60 * 60 * 1000;
    public static final String LAST_SHOW_SMS_FILE = "last_show_sms_timestamp_sp";
    public static final String LAST_SHOW_SMS_KEY = "last_show_sms_timestamp";

    private SharedPreferences sp;

    public LastShowSMSDialogTimeChecker(Context context) {
        sp = context.getSharedPreferences(LAST_SHOW_SMS_FILE, Context.MODE_PRIVATE);
    }

    public void update() {
        sp.edit().putLong(LAST_SHOW_SMS_KEY, System.currentTimeMillis()).apply();
    }

    public boolean shouldShow() {
        return (System.currentTimeMillis() - sp.getLong(LAST_SHOW_SMS_KEY, 0)) > WEEK;
    }

    public void reset() {
        sp.edit().putLong(LAST_SHOW_SMS_KEY, 0).apply();
    }
}
