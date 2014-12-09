package nz.mega.android;

import nz.mega.android.utils.Util;
import android.database.ContentObserver;
import android.os.Handler;


public class MediaObserver extends ContentObserver {

	CameraSyncService service;
	
	public MediaObserver(Handler handler, CameraSyncService service) {
		super(handler);
		this.service = service;
	}

	@Override
	public void onChange(boolean selfChange) {
		
		log("MEDIAOBSERVER");
		if (service != null){
			service.retryLater();
		}
	}
	
	public static void log(String message) {
		Util.log("MediaObserver", message);
	}

}
