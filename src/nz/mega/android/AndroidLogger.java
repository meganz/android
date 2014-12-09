package nz.mega.android;


import nz.mega.sdk.MegaLoggerInterface;
import android.util.Log;


public class AndroidLogger implements MegaLoggerInterface {
	
	 public void log(String time, int loglevel, String source, String message) {
		Log.d("AndroidLogger", source + ": " + message); 
	 }
}
