package com.mega.android;

import android.preference.PreferenceActivity;

/*
 * PinCode preference activity wrapper
 */
public class PinPreferenceActivity extends PreferenceActivity {
	
	@Override
	protected void onPause() {
		PinUtil.pause(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		PinUtil.resume(this);
	}
}
