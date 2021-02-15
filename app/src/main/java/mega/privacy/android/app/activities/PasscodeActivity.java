package mega.privacy.android.app.activities;

import android.content.res.Configuration;
import android.os.Bundle;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.utils.PasscodeUtil;

import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.Util.*;


public class PasscodeActivity extends BaseActivity {

    private PasscodeUtil passcodeUtil;
    private static long lastStart;

    /**
     * Used to control when onResume comes from a screen orientation change.
     * Since onConfigurationChanged is not implemented in all activities,
     * it cannot be used for that purpose.
     *
     * @see android.app.Activity#onConfigurationChanged(Configuration)
     */
    private boolean isScreenRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        passcodeUtil = new PasscodeUtil(this, dbH);

        if (savedInstanceState != null) {
            isScreenRotation = true;
        }
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

        if (isScreenRotation) {
            isScreenRotation = false;
        } else if (MegaApplication.getPasscodeManagement().getShowPasscodeScreen()) {
            passcodeUtil.resume();
        }

        //if leave the APP then get back, should trigger camera upload.
        if (System.currentTimeMillis() - lastStart > 1000) {
            if (megaApi.getRootNode() != null && !MegaApplication.isLoggingIn()) {
                startCameraUploadServiceIgnoreAttr(PasscodeActivity.this);
            }
        }
    }
}
