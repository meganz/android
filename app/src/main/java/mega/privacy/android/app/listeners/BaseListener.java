package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.*;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class BaseListener implements MegaRequestListenerInterface {

    // FragmentTag is for storing the fragment which has the api call within Activity Context
    protected FragmentTag fragmentTag;
    protected Context context;
    protected DatabaseHandler dBH;
    protected MegaPreferences prefs;

    public BaseListener(Context context) {
        this.context = context;
        dBH = MegaApplication.getInstance().getDbH();
        prefs = dBH.getPreferences();
    }

    public void setFragmentTag(FragmentTag fragmentTag) {
        this.fragmentTag = fragmentTag;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
