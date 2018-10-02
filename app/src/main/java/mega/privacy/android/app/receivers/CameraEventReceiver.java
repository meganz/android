package mega.privacy.android.app.receivers;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.utils.Util;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;

import static mega.privacy.android.app.utils.Constants.BOOT_JOB_ID;
import static mega.privacy.android.app.utils.JobUtil.isJobScheduled;
import static mega.privacy.android.app.utils.JobUtil.startJob;


public class CameraEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public CameraEventReceiver() {}

	@Override
	public void onReceive(final Context context, Intent intent)
	{	
		try
		{
			Cursor cursor = context.getContentResolver().query(intent.getData(), null,null, null, null);
		    cursor.moveToFirst();
		    String image_path = cursor.getString(cursor.getColumnIndex("_data"));
		    log("CameraEventReceiver_New Photo is Saved as : -" + image_path);
		}
		catch(Exception e)
		{
		    log("CameraEventReceiver_New Photo without image path");
		}
	    final Context c = context;
	    
	    handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
					c.startService(new Intent(c, CameraSyncService.class));
                } else {
                    startJob(context);
                }
			}
		}, 5 * 1000);
	    
	    
	}
	
	public static void log(String message) {
		Util.log("CameraEventReceiver", message);
	}
}