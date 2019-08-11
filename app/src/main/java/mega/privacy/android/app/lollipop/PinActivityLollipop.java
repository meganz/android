package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.PinUtil;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LocalFolderSelector;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;


public class PinActivityLollipop extends BaseActivity {
	
	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
			}
		}
	}

	@Override
	protected void onPause() {
		log("onPause");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
			}
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

		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
			}
		}

		if (megaChatApi != null){
			megaChatApi.retryPendingConnections(false, null);
		}

		if(MegaApplication.isShowPinScreen()){
			PinUtil.resume(this);
		}
	}

    protected void onRequestSDCardWritePermission(Intent intent, int resultCode) {
        if (intent == null) {
            log("intent NULL");
            if (resultCode != Activity.RESULT_OK) {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.download_requires_permission), -1);
            } else {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.donot_support_write_on_sdcard), -1);
            }
            return;
        }
        Uri treeUri = intent.getData();
        if (treeUri != null) {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir.canWrite()) {
                log("sd card root uri is " + treeUri);
                //save the sd card root uri string
                DatabaseHandler.getDbHandler(this).setUriExternalSDCard(treeUri.toString());
                LocalFolderSelector.toSelectFolder(this, null);
            }
        } else {
            log("tree uri is null!");
            Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.donot_support_write_on_sdcard), -1);
        }
    }

    protected void onSelectDownloadLocation(Intent intent,int resultCode) {
	    if(resultCode == RESULT_OK) {
            String path = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            log("select " + path + " as download location.");
            DatabaseHandler.getDbHandler(this).setStorageDownloadLocation(path);
            //TODO resume download with the new download location automatically?
        }
    }

	public static void log(String message) {
		Util.log("PinActivityLollipop", message);
	}
}
