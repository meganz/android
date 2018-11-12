package mega.privacy.android.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.JobUtil.startJob;


public class BootEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public BootEventReceiver() {}

	@Override
	public void onReceive(final Context context, Intent intent){
	    
		log("BootEventReceiver");
		final Context c = context;
	    
	    handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
                if (Util.isDeviceSupportParallelUpload()) {
                    startJob(context);
                } else {
                    c.startService(new Intent(c, CameraSyncService.class));
                }
			}
		}, 5 * 1000);
	}
	
	public static void log(String message) {
		Util.log("BootEventReceiver", message);
	}
}
