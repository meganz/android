package com.mega.android;


import android.util.Log;

import com.mega.sdk.MegaLoggerInterface;

public class AndroidLogger implements MegaLoggerInterface {
	
	 public void log(String time, int loglevel, String source, String message) {
		Log.d("AndroidLogger", source + ": " + message); 
	 }
}
