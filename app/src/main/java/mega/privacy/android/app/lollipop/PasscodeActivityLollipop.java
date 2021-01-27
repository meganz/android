package mega.privacy.android.app.lollipop;

import android.os.Bundle;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.utils.PasscodeUtil;

import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class PasscodeActivityLollipop extends BaseActivity {

	private PasscodeUtil passcodeUtil;
    private static long lastStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		passcodeUtil = new PasscodeUtil(this, dbH);
	}

	@Override
	protected void onPause() {
		passcodeUtil.pause();
		lastStart = System.currentTimeMillis();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        setAppFontSize(this);

		if(MegaApplication.getPasscodeManagement().getShowPasscodeScreen()){
			passcodeUtil.resume();
		}

		//if leave the APP then get back, should trigger camera upload.
        if(System.currentTimeMillis() - lastStart > 1000) {
			if (megaApi.getRootNode() != null && !MegaApplication.isLoggingIn()){
				startCameraUploadServiceIgnoreAttr(PasscodeActivityLollipop.this);
			}
        }
	}
}
