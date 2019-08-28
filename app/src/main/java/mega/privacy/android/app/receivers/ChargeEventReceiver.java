package mega.privacy.android.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;


public class ChargeEventReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(final Context context,Intent intent) {
        log("ChargeEventReceiver");
        startCameraUploadService(context);
    }
    
    public static void log(String message) {
        Util.log("ChargeEventReceiver",message);
    }
}