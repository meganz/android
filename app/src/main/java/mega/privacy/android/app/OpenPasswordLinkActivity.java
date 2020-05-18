package mega.privacy.android.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class OpenPasswordLinkActivity extends PinActivityLollipop implements MegaRequestListenerInterface, OnClickListener {
	
	OpenPasswordLinkActivity openPasswordLinkActivity = this;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	
	Toolbar tB;
    ActionBar aB;
	DisplayMetrics outMetrics;
	String url;
	Handler handler;
	ProgressDialog statusDialog;
	AlertDialog decryptionKeyDialog;

	RelativeLayout fragmentContainer;
	
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

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setHint(getString(R.string.password_text));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = input.getText().toString();
					if (value.trim().length() == 0) {
						finish();
						return false;
					}
					decrypt(value);
					decryptionKeyDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_ok),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.hint_set_password_protection_dialog));
		builder.setPositiveButton(getString(R.string.general_decryp),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						if (value.trim().length() == 0) {
							finish();
						}
						decrypt(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						finish();
					}
				});


		builder.setView(layout);
		decryptionKeyDialog = builder.create();
		decryptionKeyDialog.setCanceledOnTouchOutside(false);
		decryptionKeyDialog.show();
	}

	private void showKeyboardDelayed(final View view) {
		logDebug("showKeyboardDelayed");
		handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	public void decrypt(String value){

		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.general_loading));
			temp.show();
		}
		catch(Exception ex)
		{ return; }

		statusDialog = temp;

		megaApi.decryptPasswordProtectedLink(url, value, openPasswordLinkActivity);
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

}