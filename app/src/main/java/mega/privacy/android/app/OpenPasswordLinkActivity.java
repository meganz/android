package mega.privacy.android.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.RelativeLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import mega.privacy.android.app.lollipop.DecryptAlertDialog;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK;
import static mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEXS;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class OpenPasswordLinkActivity extends PinActivityLollipop
		implements MegaRequestListenerInterface, OnClickListener, DecryptAlertDialog.DecryptDialogListener {
	
	MegaApiAndroid megaApi;

	Toolbar tB;
    ActionBar aB;
	DisplayMetrics outMetrics;
	String url;
	ProgressDialog statusDialog;

	RelativeLayout fragmentContainer;
	private String mKey;

	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		setContentView(R.layout.activity_open_pass_link);
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_file_link);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		if(aB!=null){
			//		aB.setLogo(R.drawable.ic_arrow_back_black);
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setDisplayShowHomeEnabled(true);
			aB.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
			aB.setDisplayShowTitleEnabled(false);
		}

		Intent intent = getIntent();
		if (intent != null){
			url = intent.getDataString();

			askForPasswordDialog();
		}
	}

	public void askForPasswordDialog(){
		logDebug("askForPasswordDialog");

		DecryptAlertDialog.Builder builder = new DecryptAlertDialog.Builder();
		builder.setListener(this).setTitle(getString(R.string.hint_set_password_protection_dialog))
				.setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
				.setErrorMessage(R.string.invalid_link_password).setKey(mKey)
				.build().show(getSupportFragmentManager(), "decrypt");
	}

	private void decrypt(){
		if (TextUtils.isEmpty(mKey)) return;

		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.general_loading));
			temp.show();
		}
		catch(Exception ex)
		{ return; }

		statusDialog = temp;

		megaApi.decryptPasswordProtectedLink(url, mKey, this);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logDebug("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_PASSWORD_LINK){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK) {

				String decryptedLink = request.getText();

				// Folder Download link
				if (decryptedLink != null && matchRegexs(decryptedLink, FOLDER_LINK_REGEXS)) {
					logDebug("Folder link url");

					Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
					openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
					openFolderIntent.setData(Uri.parse(decryptedLink));
					startActivity(openFolderIntent);
					finish();
				}

				else if (decryptedLink != null && matchRegexs(decryptedLink, FILE_LINK_REGEXS)) {
					logDebug("Open link url");

					Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
					openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
					openFileIntent.setData(Uri.parse(decryptedLink));
					startActivity(openFileIntent);
					finish();
				}
			}
			else{
				logError("ERROR: " + e.getErrorCode());
				askForPasswordDialog();
			}
		}

	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.file_link_button_download:{

				break;
			}
			case R.id.file_link_button_import:{

				break;
			}
			case R.id.file_link_icon:{

				break;
			}
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    	// Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	finish();
		    	return true;
		    }
		}
		
		return super.onOptionsItemSelected(item);
	}


	public void showSnackbar(String s){
		showSnackbar(fragmentContainer, s);
	}

	@Override
	public void onDialogPositiveClick(String key) {
		mKey = key;
		decrypt();
	}

	@Override
	public void onDialogNegativeClick() {
		finish();
	}
}