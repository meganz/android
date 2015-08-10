package nz.mega.android;

import nz.mega.android.lollipop.ManagerActivityLollipop;
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
			Intent intent = new Intent(this, ManagerActivityLollipop.class);
			startActivity(intent);
			finish();	
		} else {
			Intent intent = new Intent(this, ManagerActivity.class);
			startActivity(intent);
			finish();
		}
		super.onCreate(savedInstanceState);
	}
}
