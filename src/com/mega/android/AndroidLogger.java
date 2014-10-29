package com.mega.android;


import android.util.Log;

import com.mega.sdk.MegaLogger;

public class AndroidLogger extends MegaLogger {
	
	 public void log(String time, int loglevel, String source, String message) {
		Log.d("AndroidLogger", source + ": " + message); 
	 }
}
