package mega.privacy.android.app;

import java.util.Locale;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


public class CreateAccountActivity extends Activity implements OnClickListener, MegaRequestListenerInterface{
	
	Button bRegister;
	Button bLogin;
	
	EditText userName;
	EditText userEmail;
	EditText userPassword;
	EditText userPasswordConfirm;
	
	CheckBox chkTOS;
	TextView tos;
	
	MegaApiAndroid megaApi;
	
	LinearLayout createAccountLayout;
	LinearLayout creatingAccountLayout;
	LinearLayout createAccountLoginLayout;
	
	TextView creatingAccountTextView;
	ProgressBar createAccountProgressBar;
	
	/*
	 * Task to process email and password
	 */
	private class HashTask extends AsyncTask<String, Void, String[]> {

		@Override
		protected String[] doInBackground(String... args) {
			String privateKey = megaApi.getBase64PwKey(args[1]);
			String publicKey = megaApi.getStringHash(privateKey, args[0]);
			return new String[]{new String(privateKey), new String(publicKey)}; 
		}
		
		@Override
		protected void onPostExecute(String[] key) {
			onKeysGenerated(key[0], key[1]);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_account);	
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    
	    float dpHeight = outMetrics.heightPixels / density;
	    float dpWidth  = outMetrics.widthPixels / density;
	    
		megaApi = ((MegaApplication) getApplication()).getMegaApi();
		
		TextView tos = (TextView)findViewById(R.id.tos);
		tos.setTextColor(getResources().getColor(R.color.mega));
//		Spanned spanned = Html.fromHtml(getString(R.string.tos));
//		tos.setMovementMethod(LinkMovementMethod.getInstance());
//		tos.setText(spanned);
//		tos.setLinkTextColor(getResources().getColor(R.color.mega));
		tos.setOnClickListener(this);
		
		bRegister = (Button) findViewById(R.id.button_create_account_create);
		bLogin = (Button) findViewById(R.id.button_login_create);
		bRegister.setOnClickListener(this);
		bLogin.setOnClickListener(this);
		
		userName = (EditText) findViewById(R.id.create_account_name_text);
		userEmail = (EditText) findViewById(R.id.create_account_email_text);
		userPassword = (EditText) findViewById(R.id.create_account_password_text);
		userPasswordConfirm = (EditText) findViewById(R.id.create_account_password_text_confirm);
		
		chkTOS = (CheckBox) findViewById(R.id.create_account_chkTOS);
		tos = (TextView) findViewById(R.id.tos);
		float scaleText;
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }
	    tos.setTextSize(TypedValue.COMPLEX_UNIT_SP, (8*scaleText));
		
		createAccountLayout = (LinearLayout) findViewById(R.id.create_account_create_layout);
		createAccountLoginLayout = (LinearLayout) findViewById(R.id.create_account_login_layout);
		creatingAccountLayout = (LinearLayout) findViewById(R.id.create_account_creating_layout);
		creatingAccountTextView = (TextView) findViewById(R.id.create_account_creating_text);
		createAccountProgressBar = (ProgressBar) findViewById(R.id.create_account_progress_bar);
		
		createAccountLayout.setVisibility(View.VISIBLE);
		createAccountLoginLayout.setVisibility(View.VISIBLE);
		creatingAccountLayout.setVisibility(View.GONE);
		creatingAccountTextView.setVisibility(View.GONE);
		createAccountProgressBar.setVisibility(View.GONE);
		
//		TextView termsView = (TextView) findViewById(R.id.terms);
//		termsView.setText(R.string.tos2);
//		termsView.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				String url = "http://g.static.mega.co.nz/pages/terms.html";
//				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//				startActivity(intent);
//			}
//		});
	}
	
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.button_create_account_create:
				onCreateAccountClick(v);
				break;
				
			case R.id.button_login_create:
				onLoginClick(v);
				break;
			
			case R.id.tos:
//				Intent browserIntent = new Intent(Intent.ACTION_VIEW);
//				browserIntent.setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
//				browserIntent.setDataAndType(Uri.parse("http://www.google.es"), "text/html");
//				browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
//				startActivity(browserIntent);
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				viewIntent.setData(Uri.parse("https://mega.co.nz/mobile_privacy.html"));
				startActivity(viewIntent);
				break;
		}		
	}
	
	@Override
	public void onBackPressed(){
		Intent intent = new Intent(this, TourActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void onLoginClick(View v){
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
	
	public void onCreateAccountClick (View v){
		submitForm();
	}
	
	/*
	 * Registration form submit
	 */
	private void submitForm() {
		log("submit form!");

//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		dbH.clearCredentials();
		megaApi.logout();
		
		if (!validateForm()) {
			return;
		}
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(userEmail.getWindowToken(), 0);
		
		if(!Util.isOnline(this))
		{
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),
					false, this);
			return;
		}
		
		createAccountLayout.setVisibility(View.GONE);
		createAccountLoginLayout.setVisibility(View.GONE);
		creatingAccountLayout.setVisibility(View.VISIBLE);
		creatingAccountTextView.setVisibility(View.GONE);
		createAccountProgressBar.setVisibility(View.VISIBLE);
		
		new HashTask().execute(userEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim(), userPassword.getText().toString());
	}
	
	private boolean validateForm() {
		String emailError = getEmailError();
		String passwordError = getPasswordError();
		String usernameError = getUsernameError();
		String passwordConfirmError = getPasswordConfirmError();

		// Set or remove errors
		userName.setError(usernameError);
		userEmail.setError(emailError);
		userPassword.setError(passwordError);
		userPasswordConfirm.setError(passwordConfirmError);

		// Return false on any error or true on success
		if (usernameError != null) {
			userName.requestFocus();
			return false;
		} else if (emailError != null) {
			userEmail.requestFocus();
			return false;
		} else if (passwordError != null) {
			userPassword.requestFocus();
			return false;
		} else if (passwordConfirmError != null) {
			userPasswordConfirm.requestFocus();
			return false;
		} else if (!chkTOS.isChecked()) {
			Util.showErrorAlertDialog(
					getString(R.string.create_account_no_terms), false, this);
			return false;
		}
		return true;
	}
	
	private String getEmailError() {
		String value = userEmail.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}

	private String getUsernameError() {
		String value = userName.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_username);
		}
		return null;
	}

	private String getPasswordError() {
		String value = userPassword.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		} else if (value.length() < 5) {
			return getString(R.string.error_short_password);
		}
		return null;
	}

	private String getPasswordConfirmError() {
		String password = userPassword.getText().toString();
		String confirm = userPasswordConfirm.getText().toString();
		if (password.equals(confirm) == false) {
			return getString(R.string.error_passwords_dont_match);
		}
		return null;
	}
	
	private void onKeysGenerated(final String privateKey, final String publicKey) {
		if(!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),
					false, this);
			return;
		}
		
		createAccountLayout.setVisibility(View.GONE);
		createAccountLoginLayout.setVisibility(View.GONE);
		creatingAccountLayout.setVisibility(View.VISIBLE);
		creatingAccountTextView.setVisibility(View.VISIBLE);
		createAccountProgressBar.setVisibility(View.VISIBLE);
		megaApi.createAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), userPassword.getText().toString(), userName.getText().toString(), this);
//		megaApi.fastCreateAccount(userEmail.getText().toString().trim().toLowerCase(Locale.ENGLISH), privateKey, userName.getText().toString().trim(), this);
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart" + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (e.getErrorCode() != MegaError.API_OK) {
			log("ERROR CODE: " + e.getErrorCode() + "_ ERROR MESSAGE: " + e.getErrorString());
			String message = e.getErrorString();
			if (e.getErrorCode() == MegaError.API_EEXIST) {
				Intent intent = new Intent(this, LoginActivity.class);
				intent.setAction(LoginActivity.ACTION_CREATE_ACCOUNT_EXISTS);
				startActivity(intent);
				finish();
				return;
			}
			Util.showErrorAlertDialog(message, false, CreateAccountActivity.this);			
			
			createAccountLayout.setVisibility(View.VISIBLE);
			createAccountLoginLayout.setVisibility(View.VISIBLE);
			creatingAccountLayout.setVisibility(View.GONE);
			creatingAccountTextView.setVisibility(View.GONE);
			createAccountProgressBar.setVisibility(View.GONE);
			
			return;
		}
		onRegister();
	}
	
	private void onRegister() {
//		android.content.DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				dialog.dismiss();
//				Intent intent = new Intent(CreateAccountActivity.this, TourActivity.class);
//				startActivity(intent);
//				finish();
//			}
//		};
//
//		AlertDialog.Builder alert = Util
//				.getCustomAlertBuilder(this,
//						getString(R.string.create_account_confirm_title),
//						getString(R.string.create_account_confirm), null)
//				.setPositiveButton(getString(android.R.string.ok), listener)
//				.setCancelable(false);
//		alert.show();

		Intent intent = new Intent(CreateAccountActivity.this, ConfirmEmailActivity.class);
		startActivity(intent);
		finish();
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
		log ("onRequestTemporaryError");
	}
	
	public static void log(String log) {
		Util.log("CreateAccountActivity", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
}
