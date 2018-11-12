package mega.privacy.android.app.receivers;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.utils.Util;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import static mega.privacy.android.app.utils.JobUtil.startJob;


public class ChargeEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public ChargeEventReceiver() {}

	@Override
	public void onReceive(Context context, Intent intent){
	    
		log("ChargeEventReceiver");
		final Context c = context;
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
				if (Util.isDeviceSupportParallelUpload()) {
                    startJob(c);
                } else {
                    c.startService(new Intent(c, CameraSyncService.class));
                }
			}
		}, 5 * 1000);
	}
	
	public static void log(String message) {
		Util.log("ChargeEventReceiver", message);
	}
}