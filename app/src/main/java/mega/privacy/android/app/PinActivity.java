package mega.privacy.android.app;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class PinActivity extends AppCompatActivity{
	
	private MegaApiAndroid megaApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
	}

	@Override
	protected void onPause() {
		log("onPause");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		PinUtil.pause(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		log("onResume");
		super.onResume();
		Util.setAppFontSize(this);
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		megaApi.retryPendingConnections();
		
		PinUtil.resume(this);
	}
	
	public static void log(String message) {
		Util.log("PinActivity", message);
	}
}
