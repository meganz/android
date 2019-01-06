package mega.privacy.android.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.utils.Util;


public class ChargeEventReceiver extends BroadcastReceiver {
	
	public ChargeEventReceiver() {}

	@Override
	public void onReceive(final Context context,Intent intent){
	    
		log("ChargeEventReceiver");
        try {
            if (!CameraUploadsService.isServiceRunning) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    log("ChargeEventReceiver: starting on below Oreo: ");
                    Intent newIntent = new Intent(context,CameraUploadsService.class);
                    context.startService(newIntent);
                } else {
                    log("ChargeEventReceiver: above Oreo do nothing ");
                }
            } else {
                log("ChargeEventReceiver: camera upload service has been started");
            }
        } catch (Exception e) {
            log("ChargeEventReceiver: Exception: " + e.getMessage() + "_" + e.getStackTrace());
        }
	}
	
	public static void log(String message) {
		Util.log("ChargeEventReceiver", message);
	}
}