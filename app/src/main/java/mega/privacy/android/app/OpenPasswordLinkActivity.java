package mega.privacy.android.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
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
import static mega.privacy.android.app.utils.Util.showKeyboardDelayed;

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
	private String mPassword;
	private View mErrorView;
	private EditText mPasswordEdit;

	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mPasswordEdit != null) {
			showKeyboardDelayed(mPasswordEdit);
		}
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

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_with_error_hint, null);

		builder.setView(v).setPositiveButton(R.string.general_decryp, null)
				.setNegativeButton(R.string.general_cancel, null);

		TextView titleView = v.findViewById(R.id.title);
		titleView.setText(getString(R.string.hint_set_password_protection_dialog));
		mPasswordEdit = v.findViewById(R.id.text);
		mPasswordEdit.setSingleLine();
		mErrorView = v.findViewById(R.id.error);
		((TextView)v.findViewById(R.id.error_text)).setText(R.string.invalid_link_password);

		if (TextUtil.isTextEmpty(mPassword)) {
			mPasswordEdit.setHint(getString(R.string.password_text));
			mPasswordEdit.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		} else {
			showErrorMessage();
		}

		mPasswordEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
		mPasswordEdit.setOnEditorActionListener((v1, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				tryToDecrypt();
				return true;
			}
			return false;
		});
		mPasswordEdit.setImeActionLabel(getString(R.string.general_ok),EditorInfo.IME_ACTION_DONE);
		mPasswordEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void afterTextChanged(Editable s) {
				hideErrorMessage();
			}
		});

		decryptionKeyDialog = builder.create();
		decryptionKeyDialog.setCanceledOnTouchOutside(false);
		decryptionKeyDialog.show();

		// Set onClickListeners for buttons after showing the dialog would prevent
		// the dialog from dismissing automatically on clicking the buttons
		decryptionKeyDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
			tryToDecrypt();
		});
		decryptionKeyDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
				.setOnClickListener((view) -> finish());

		showKeyboardDelayed(mPasswordEdit);
	}

	private void tryToDecrypt() {
		mPassword = mPasswordEdit.getText().toString();
		if (mPassword.trim().length() == 0) {
			mPassword = "";
			showErrorMessage();
			return;
		}

		decrypt(mPassword);
		decryptionKeyDialog.dismiss();
	}

	private void showErrorMessage() {
		if (mPasswordEdit == null || mErrorView == null) return;

		mPasswordEdit.setText(mPassword);
		mPasswordEdit.setSelectAllOnFocus(true);
		mPasswordEdit.setTextColor(ContextCompat.getColor(this, R.color.dark_primary_color));
		mPasswordEdit.getBackground().mutate().clearColorFilter();
		mPasswordEdit.getBackground().mutate().setColorFilter(ContextCompat.getColor(
				this, R.color.dark_primary_color), PorterDuff.Mode.SRC_ATOP);

		mErrorView.setVisibility(View.VISIBLE);
	}

	private void hideErrorMessage() {
		if (mPasswordEdit == null || mErrorView == null) return;

		mErrorView.setVisibility(View.GONE);
		mPasswordEdit.setTextColor(ContextCompat.getColor(
				OpenPasswordLinkActivity.this, R.color.name_my_account));
		mPasswordEdit.getBackground().mutate().clearColorFilter();
		mPasswordEdit.getBackground().mutate().setColorFilter(ContextCompat.getColor(
				OpenPasswordLinkActivity.this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
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