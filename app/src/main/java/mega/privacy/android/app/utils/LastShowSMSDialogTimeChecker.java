package mega.privacy.android.app.utils;


import android.content.Context;
import android.content.SharedPreferences;

import mega.privacy.android.app.MegaApplication;

import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class LastShowSMSDialogTimeChecker {

    private static final long WEEK = 7 * 24 * 60 * 60 * 1000;
    private static final long FORTNIGHT = 2 * WEEK;
    private static final long MONTH = 2 * FORTNIGHT;

    private static final String LAST_SHOW_SMS_FILE = "last_show_sms_timestamp_sp";

    /**
     * The timestamp of last showing
     */
    private static final String LAST_SHOW_SMS_KEY = "last_show_sms_timestamp";

    /**
     * How many times the dialog has shown in a time period
     */
    private static final String TIMES = "times";

    /**
     * Time period type, it decides the max times the dialog will show with the time period
     */
    private static final String TYPE = "type";

    private static final int TYPE_WEEK = 0;
    private static final int TYPE_FORTNIGHT = 1;
    private static final int TYPE_MONTH = 2;

    /**
     * How many times the dialog can show under the time period type.
     * When exceeds, turn to next type.
     */
    private static final int TIMES_WEEK = 5;
    private static final int TIMES_FORTNIGHT = 5;

    private SharedPreferences sp;

    public LastShowSMSDialogTimeChecker(Context context) {
        sp = context.getSharedPreferences(LAST_SHOW_SMS_FILE, Context.MODE_PRIVATE);
    }

    public void update() {
        int times = sp.getInt(TIMES, 1);
        int type = sp.getInt(TYPE, TYPE_WEEK);
        long currentTime = System.currentTimeMillis();

        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(LAST_SHOW_SMS_KEY, currentTime);

        switch (type) {
            case TYPE_WEEK:
                if (times < TIMES_WEEK) {
                    editor.putInt(TIMES, times + 1);
                } else {
                    // update type, reset times
                    editor.putInt(TIMES, 1).putInt(TYPE, TYPE_FORTNIGHT);
                }
                break;
            case TYPE_FORTNIGHT:
                if (times < TIMES_FORTNIGHT) {
                    editor.putInt(TIMES, times + 1);
                } else {
                    // only update type
                    editor.putInt(TYPE, TYPE_MONTH);
                }
                break;
        }

        editor.apply();
    }

    public boolean shouldShow() {
        // If account is in ODQ Paywall state avoid ask for SMS verification because request will fail.
        if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
            return false;
        }

        int type = sp.getInt(TYPE, TYPE_WEEK);
        long currentTime = System.currentTimeMillis();
        switch (type) {
            case TYPE_WEEK:
                return (currentTime - sp.getLong(LAST_SHOW_SMS_KEY, 0)) > WEEK;
            case TYPE_FORTNIGHT:
                return (currentTime - sp.getLong(LAST_SHOW_SMS_KEY, 0)) > FORTNIGHT;
            case TYPE_MONTH:
                return (currentTime - sp.getLong(LAST_SHOW_SMS_KEY, 0)) > MONTH;
            default:
                return false;
        }
    }

    public void reset() {
        sp.edit().clear().apply();
    }
}
