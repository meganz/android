package com.mega.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.mega.android.CameraSyncService;
import com.mega.android.utils.Util;

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