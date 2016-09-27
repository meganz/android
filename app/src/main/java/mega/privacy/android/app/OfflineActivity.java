package mega.privacy.android.app;

import mega.privacy.android.app.utils.Util;
import android.content.Intent;
import android.os.Bundle;


public class OfflineActivity extends PinActivity{

	
	boolean isListOffline = true;

	String pathNavigation = "/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);


	}

	
	public static void log(String message) {
		Util.log("OfflineActivity", message);	
	}
}
