package mega.privacy.android.app.lollipop;

import android.os.Bundle;
import android.os.Handler;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.PinUtil;

import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class PinActivityLollipop extends BaseActivity {

    private static long lastStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		PinUtil.pause(this);
		lastStart = System.currentTimeMillis();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        setAppFontSize(this);

		if(MegaApplication.isShowPinScreen()){
			PinUtil.resume(this);
		}

		//if leave the APP then get back, should trigger camera upload.
        if(System.currentTimeMillis() - lastStart > 1000) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCameraUploadService(PinActivityLollipop.this);
                }
            }, 3000);
        }
	}
}
