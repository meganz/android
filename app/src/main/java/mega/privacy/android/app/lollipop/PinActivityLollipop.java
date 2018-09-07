package mega.privacy.android.app.lollipop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.PinUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;


public class PinActivityLollipop extends AppCompatActivity{
	
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

		MegaApplication.activityPaused();

		super.onPause();
	}
	
	@Override
	protected void onResume() {
		log("onResume");

		super.onResume();
        Util.setAppFontSize(this);
		MegaApplication.activityResumed();

		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		megaApi.retryPendingConnections();

		if(MegaApplication.isShowPinScreen()){
			PinUtil.resume(this);
		}

		((MegaApplication) getApplication()).sendSignalPresenceActivity();
	}

	public static void log(String message) {
		Util.log("PinActivityLollipop", message);
	}
}
