package mega.privacy.android.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.JobUtil.rescheduleCameraUpload;

public class BootEventReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(final Context context,Intent intent) {
        LogUtil.logDebug("BootEventReceiver");
        rescheduleCameraUpload(context);
    }
}
