package com.mega.android;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class PinActivity extends ActionBarActivity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		log("onPause");
		PinUtil.pause(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();
		PinUtil.resume(this);
	}
	
	public static void log(String message) {
		Util.log("PinActivity", message);
	}
}
