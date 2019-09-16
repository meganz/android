package mega.privacy.android.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class NetworkTypeChangeReceiver extends BroadcastReceiver {

    private OnNetworkTypeChangeCallback callback;

    public static final int MOBILE = 0;
    public static final int WIFI = 1;

    @Override
    public void onReceive(Context context,Intent intent) {
        LogUtil.logDebug("Network changes: " + intent.getAction());
        if("android.net.conn.CONNECTIVITY_CHANGE".equalsIgnoreCase(intent.getAction())) {
            int type = Util.isOnWifi(context) ? WIFI : MOBILE;
            LogUtil.logDebug("Network type: " + type);
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
