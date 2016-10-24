package mega.privacy.android.app;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public class LauncherActivity extends PinActivity{
	
	ManagerActivityLollipop mActivityLol;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		Intent intent = new Intent(this, ManagerActivityLollipop.class);
		startActivity(intent);
		finish();
		super.onCreate(savedInstanceState);
	}
	
	public static void log(String message) {
		Util.log("LauncherActivity", message);
	}
}
