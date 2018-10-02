package mega.privacy.android.app.receivers;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.jobservices.BootJobService;
import mega.privacy.android.app.utils.Util;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.format.DateUtils;

import java.util.List;

import static mega.privacy.android.app.utils.Constants.BOOT_JOB_ID;
import static mega.privacy.android.app.utils.JobUtil.isJobScheduled;
import static mega.privacy.android.app.utils.JobUtil.startJob;
import static mega.privacy.android.app.utils.Util.logJobState;


public class BootEventReceiver extends BroadcastReceiver {
	
	Handler handler = new Handler();
	
	public BootEventReceiver() {}

	@Override
	public void onReceive(final Context context, Intent intent){
//		Cursor cursor = context.getContentResolver().query(intent.getData(), null,null, null, null);
//	    cursor.moveToFirst();
//	    String image_path = cursor.getString(cursor.getColumnIndex("_data"));
//	    log("CameraEventReceiver_New Photo is Saved as : -" + image_path);
	    
		log("BootEventReceiver");
		final Context c = context;
	    
	    handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				log("Now I start the service");
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
					c.startService(new Intent(c, CameraSyncService.class));
                } else if (!isJobScheduled(context,BOOT_JOB_ID)) {
                    startJob(context);
                }
			}
		}, 5 * 1000);
	}
	
	public static void log(String message) {
		Util.log("BootEventReceiver", message);
	}
}
