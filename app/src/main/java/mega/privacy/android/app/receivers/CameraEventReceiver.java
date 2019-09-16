package mega.privacy.android.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.utils.JobUtil;


public class CameraEventReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(final Context context,Intent intent) {
        JobUtil.startCameraUploadService(context);
    }
}