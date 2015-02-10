package nz.mega.android.receivers;

import nz.mega.android.CameraSyncService;
import nz.mega.android.utils.Util;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;


public class ChargeEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public ChargeEventReceiver() {}

	@Override
	public void onReceive(Context context, Intent intent){
//		Cursor cursor = context.getContentResolver().query(intent.getData(), null,null, null, null);
//	    cursor.moveToFirst();
//	    String image_path = cursor.getString(cursor.getColumnIndex("_data"));
//	    log("CameraEventReceiver_New Photo is Saved as : -" + image_path);
	    
		log("ChargeEventReceiver");
		final Context c = context;
		
		log("isCharging(): "+Util.isCharging(c));
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
				c.startService(new Intent(c, CameraSyncService.class));		
			}
		}, 5 * 1000);
	}
	
	public static void log(String message) {
		Util.log("ChargeEventReceiver", message);
	}
}