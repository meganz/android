package mega.privacy.android.app.receivers;

import static mega.privacy.android.app.utils.Util.isOnWifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

public class NetworkTypeChangeReceiver extends BroadcastReceiver {

    private OnNetworkTypeChangeCallback callback;

    public static final int MOBILE = 0;
    public static final int WIFI = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("Network changes: %s", intent.getAction());
        if ("android.net.conn.CONNECTIVITY_CHANGE".equalsIgnoreCase(intent.getAction())) {
            int type = isOnWifi(context) ? WIFI : MOBILE;
            Timber.d("Network type: %s", type);
            if (callback != null) {
                callback.onTypeChanges(type);
            }
        }
    }

    public void setCallback(OnNetworkTypeChangeCallback callback) {
        this.callback = callback;
    }

    public interface OnNetworkTypeChangeCallback {

        void onTypeChanges(int type);

    }
}
