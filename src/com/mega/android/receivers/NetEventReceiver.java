package com.mega.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.widget.Toast;

import com.mega.android.CameraSyncService;
import com.mega.android.DatabaseHandler;
import com.mega.android.MegaApplication;
import com.mega.android.MegaAttributes;
import com.mega.android.Util;

public class NetEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public NetEventReceiver() {}

	@Override
	public void onReceive(Context context, Intent intent){
//		Cursor cursor = context.getContentResolver().query(intent.getData(), null,null, null, null);
//	    cursor.moveToFirst();
//	    String image_path = cursor.getString(cursor.getColumnIndex("_data"));
//	    log("New Photo is Saved as : -" + image_path);
	    
		log("NetEventReceiver");
		final Context c = context;
		
		DatabaseHandler dbH = new DatabaseHandler(context);
		MegaAttributes attr = dbH.getAttributes();
		
		if (attr != null){
			if (attr.getOnline() != null){
				if (Boolean.parseBoolean(attr.getOnline())){
					if (!Util.isOnline(context)){
						Toast.makeText(context, "REFRESCAR, porque antes era online y ahora offline", Toast.LENGTH_LONG).show();
					}
				}
				else{
					if (Util.isOnline(context)){
						Toast.makeText(context, "REFRESCAR, porque antes era offline y ahora online", Toast.LENGTH_LONG).show();
					}
				}
			}
		}
		
		if (Util.isOnline(context)){
			dbH.setAttrOnline(true);
		}
		else{
			dbH.setAttrOnline(false);
		}
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
				c.startService(new Intent(c, CameraSyncService.class));		
			}
		}, 5 * 60 * 1000);
	}
	
	public static void log(String message) {
		Util.log("NetEventReceiver", message);
	}
}