package nz.mega.android;

import nz.mega.android.lollipop.ManagerActivityLollipop;
import nz.mega.android.utils.Util;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public class LauncherActivity extends PinActivity{
	
	ManagerActivity mActivity;
	ManagerActivityLollipop mActivityLol;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
			log("Lollipop Version");
			Intent intent = new Intent(this, ManagerActivityLollipop.class);
			startActivity(intent);
			finish();	
		} else {
			log("Older Version");
			Intent intent = new Intent(this, ManagerActivity.class);
			startActivity(intent);
			finish();
		}
		super.onCreate(savedInstanceState);
	}
	
	public static void log(String message) {
		Util.log("LauncherActivity", message);
	}
}
