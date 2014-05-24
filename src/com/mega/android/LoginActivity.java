package com.mega.android;

import java.util.Locale;

import com.mega.components.MySwitch;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListener;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Credentials;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener, MegaRequestListenerInterface{
	
	public static String ACTION_REFRESH = "ACTION_REFRESH";
	public static String ACTION_CREATE_ACCOUNT_EXISTS = "ACTION_CREATE_ACCOUNT_EXISTS";
	public static String ACTION_CONFIRM = "MEGA_ACTION_CONFIRM";
	public static String EXTRA_CONFIRMATION = "MEGA_EXTRA_CONFIRMATION";
	
	TextView loginTitle;
	EditText et_user;
	EditText et_password;
	Button bRegister;
	Button bLogin;
	ImageView loginThreeDots;
	MySwitch loginSwitch;
	TextView loginABC;
	LinearLayout loginLogin;
	LinearLayout loginLoggingIn;
	LinearLayout loginCreateAccount;
	View loginDelimiter;
	ProgressBar loginProgressBar;
	TextView generatingKeysText;
	TextView queryingSignupLinkText;
	TextView confirmingAccountText;
	TextView loggingInText;
	TextView fetchingNodesText;
	
	int heightGrey = 0;
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
//	private ProgressDialog progress;
	
	private String lastEmail;
	private String lastPassword;
	private String gPublicKey;
	private String gPrivateKey;
	
	private String confirmLink;
	
	static LoginActivity loginActivity;
    private MegaApiAndroid megaApi;
    private MegaRequestListener requestListener;
    UserCredentials credentials;
    private boolean backWhileLogin;
    private boolean loginClicked = false;
    private long parentHandle = -1;
    
    String action = null;
    String url = null;
	
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
		
		loginClicked = false;
		
		loginActivity = this;
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		backWhileLogin = false;
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
	    
	    heightGrey = (int) (Util.percScreenLogin * outMetrics.heightPixels);
	    
	    setContentView(R.layout.activity_login);
		
	    loginTitle = (TextView) findViewById(R.id.login_text_view);
		loginLogin = (LinearLayout) findViewById(R.id.login_login_layout);
		loginLoggingIn = (LinearLayout) findViewById(R.id.login_logging_in_layout);
		loginCreateAccount = (LinearLayout) findViewById(R.id.login_create_account_layout);
		loginDelimiter = (View) findViewById(R.id.login_delimiter);
		loginProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
		generatingKeysText = (TextView) findViewById(R.id.login_generating_keys_text);
		queryingSignupLinkText = (TextView) findViewById(R.id.login_query_signup_link_text);
		confirmingAccountText = (TextView) findViewById(R.id.login_confirm_account_text);
		loggingInText = (TextView) findViewById(R.id.login_logging_in_text);
		fetchingNodesText = (TextView) findViewById(R.id.login_fetch_nodes_text);
		
		loginTitle.setText(R.string.login_activity);
		loginTitle.setTextSize(28*scaleH);
		
		loginLogin.setVisibility(View.VISIBLE);
		loginCreateAccount.setVisibility(View.VISIBLE);
		loginDelimiter.setVisibility(View.VISIBLE);
		loginLoggingIn.setVisibility(View.GONE);
		generatingKeysText.setVisibility(View.GONE);
		loggingInText.setVisibility(View.GONE);
		fetchingNodesText.setVisibility(View.GONE);
		queryingSignupLinkText.setVisibility(View.GONE);
		confirmingAccountText.setVisibility(View.GONE);
		
		et_user = (EditText) findViewById(R.id.login_email_text);
		et_password = (EditText) findViewById(R.id.login_password_text);
		et_password.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					submitForm();
					return true;
				}
				return false;
			}
		});
		
		bRegister = (Button) findViewById(R.id.button_create_account_login);
		bLogin = (Button) findViewById(R.id.button_login_login);
		bRegister.setOnClickListener(this);		
		bLogin.setOnClickListener(this);
		
		loginLogin.setPadding(0, Util.px2dp((40*scaleH), outMetrics), 0, Util.px2dp((40*scaleH), outMetrics));
		
		((LinearLayout.LayoutParams)bLogin.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((3*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), Util.px2dp((5*scaleH), outMetrics));
		((LinearLayout.LayoutParams)bRegister.getLayoutParams()).setMargins(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((3*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), Util.px2dp((5*scaleH), outMetrics));
		
		loginThreeDots = (ImageView) findViewById(R.id.login_three_dots);
		
		loginThreeDots.setPadding(0, Util.px2dp((20*scaleH), outMetrics), Util.px2dp((4*scaleW), outMetrics), Util.px2dp((3*scaleH), outMetrics));
		
		loginABC = (TextView) findViewById(R.id.ABC);
		
		((TableRow.LayoutParams)loginABC.getLayoutParams()).setMargins(0, 0, 0, Util.px2dp((5*scaleH), outMetrics));
		
		loginSwitch = (MySwitch) findViewById(R.id.switch_login);
		loginSwitch.setChecked(true);
		
		loginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
						et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
						et_password.setSelection(et_password.getText().length());
				}else{
						et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						et_password.setSelection(et_password.getText().length());
			    }				
			}
		});
		
		((TableRow.LayoutParams)loginSwitch.getLayoutParams()).setMargins(Util.px2dp((1*scaleH), outMetrics), Util.px2dp((8*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0);
		
		Intent intentReceived = getIntent();
		if (intentReceived != null){
			if (ACTION_CONFIRM.equals(intentReceived.getAction())) {
				handleConfirmationIntent(intentReceived);
				return;
			}
			else if (ACTION_CREATE_ACCOUNT_EXISTS.equals(intentReceived.getAction())){
				String message = getString(R.string.error_email_registered);
				Util.showErrorAlertDialog(message, false, LoginActivity.this);
				return;
			}
		}
		
		credentials = Preferences.getCredentials(this);
		if (credentials != null){
			if ((intentReceived != null) && (intentReceived.getAction() != null)){
				if (intentReceived.getAction().equals(ACTION_REFRESH)){
					parentHandle = intentReceived.getLongExtra("PARENT_HANDLE", -1);
					loginLogin.setVisibility(View.GONE);
					loginDelimiter.setVisibility(View.GONE);
					loginCreateAccount.setVisibility(View.GONE);
					queryingSignupLinkText.setVisibility(View.GONE);
					confirmingAccountText.setVisibility(View.GONE);
					loginLoggingIn.setVisibility(View.VISIBLE);
					generatingKeysText.setVisibility(View.VISIBLE);
					lastEmail = credentials.getEmail();
					gPublicKey = credentials.getPublicKey();
					gPrivateKey = credentials.getPrivateKey();
					loggingInText.setVisibility(View.VISIBLE);
					megaApi.fastLogin(lastEmail, gPublicKey, gPrivateKey, this);
					return;
				}
				else{
					if (intentReceived.getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK)){
						action = ManagerActivity.ACTION_OPEN_MEGA_LINK;
						url = intentReceived.getDataString();
					}
					
					MegaNode rootNode = megaApi.getRootNode();
					if (rootNode != null){
						Intent intent = new Intent(this, ManagerActivity.class);
						if (action != null){
							intent.setAction(action);
							intent.setData(Uri.parse(url));
						}
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
						}
						this.startActivity(intent);
						this.finish();
						return;
					}
					else{
						loginLogin.setVisibility(View.GONE);
						loginDelimiter.setVisibility(View.GONE);
						loginCreateAccount.setVisibility(View.GONE);
						queryingSignupLinkText.setVisibility(View.GONE);
						confirmingAccountText.setVisibility(View.GONE);
						loginLoggingIn.setVisibility(View.VISIBLE);
						generatingKeysText.setVisibility(View.VISIBLE);
						lastEmail = credentials.getEmail();
						gPublicKey = credentials.getPublicKey();
						gPrivateKey = credentials.getPrivateKey();
						loggingInText.setVisibility(View.VISIBLE);
						megaApi.fastLogin(lastEmail, gPublicKey, gPrivateKey, this);
						return;
					}
				}
			}
			else{
				MegaNode rootNode = megaApi.getRootNode();
				if (rootNode != null){
					Intent intent = new Intent(this, ManagerActivity.class);
					if (action != null){
						intent.setAction(action);
						intent.setData(Uri.parse(url));
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					}
					this.startActivity(intent);
					this.finish();
					return;
				}
				else{
					loginLogin.setVisibility(View.GONE);
					loginDelimiter.setVisibility(View.GONE);
					loginCreateAccount.setVisibility(View.GONE);
					queryingSignupLinkText.setVisibility(View.GONE);
					confirmingAccountText.setVisibility(View.GONE);
					loginLoggingIn.setVisibility(View.VISIBLE);
					generatingKeysText.setVisibility(View.VISIBLE);
					lastEmail = credentials.getEmail();
					gPublicKey = credentials.getPublicKey();
					gPrivateKey = credentials.getPrivateKey();
					loggingInText.setVisibility(View.VISIBLE);
					megaApi.fastLogin(lastEmail, gPublicKey, gPrivateKey, this);
					return;
				}
			}
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		int diffHeight = heightGrey - loginCreateAccount.getTop();
		
		int paddingBottom = Util.px2dp((40*scaleH), outMetrics) + diffHeight;
		loginLogin.setPadding(0, Util.px2dp((40*scaleH), outMetrics), 0, paddingBottom);
		
//		Toast.makeText(this, "onWindow: HEIGHT: " + loginCreateAccount.getTop() +"____" + heightGrey, Toast.LENGTH_LONG).show();
//		int marginBottom = 37; //related to a 533dp height
//		float dpHeight = outMetrics.heightPixels / density;
//		marginBottom =  marginBottom + (int) ((dpHeight - 533) / 6);
//		loginLogin.setPadding(0, Util.px2dp((40*scaleH), outMetrics), 0, Util.px2dp((marginBottom*scaleH), outMetrics));
	}

	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.button_login_login:
				loginClicked = true;
				onLoginClick(v);
				break;
			case R.id.button_create_account_login:
				onRegisterClick(v);
				break;
		}
	}
	
	public void onLoginClick(View v){
		submitForm();
	}
	
	public void onRegisterClick(View v){
		Intent intent = new Intent(this, CreateAccountActivity.class);
		startActivity(intent);
		finish();
	}
	
	/*
	 * Log in form submit
	 */
	private void submitForm() {
		if (!validateForm()) {
			return;
		}
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(et_user.getWindowToken(), 0);
		
		if(!Util.isOnline(this))
		{
			loginLoggingIn.setVisibility(View.GONE);
			loginLogin.setVisibility(View.VISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.VISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem),false, this);
			return;
		}
		
		loginLogin.setVisibility(View.GONE);
		loginDelimiter.setVisibility(View.GONE);
		loginCreateAccount.setVisibility(View.GONE);
		loginLoggingIn.setVisibility(View.VISIBLE);
		generatingKeysText.setVisibility(View.VISIBLE);
		queryingSignupLinkText.setVisibility(View.GONE);
		confirmingAccountText.setVisibility(View.GONE);
		
		lastEmail = et_user.getText().toString().toLowerCase(Locale.ENGLISH).trim();
		lastPassword = et_password.getText().toString();
		
		log("generating keys");
		
		new HashTask().execute(lastEmail, lastPassword);
	}
	
	private void onKeysGenerated(String privateKey, String publicKey) {
		log("key generation finished");

		this.gPrivateKey = privateKey;
		this.gPublicKey = publicKey;
		
		if (confirmLink == null) {
			onKeysGeneratedLogin(privateKey, publicKey);
		} 
		else{
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), true, this);
				return;
			}
			
			loginLogin.setVisibility(View.GONE);
			loginDelimiter.setVisibility(View.GONE);
			loginCreateAccount.setVisibility(View.GONE);
			loginLoggingIn.setVisibility(View.VISIBLE);
			generatingKeysText.setVisibility(View.VISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.VISIBLE);
			fetchingNodesText.setVisibility(View.GONE);
			
			log("fastConfirm");
			megaApi.fastConfirmAccount(confirmLink, privateKey, this);
		}
	}
	
	private void onKeysGeneratedLogin(final String privateKey, final String publicKey) {
		
		if(!Util.isOnline(this)){
			loginLoggingIn.setVisibility(View.GONE);
			loginLogin.setVisibility(View.VISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.VISIBLE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			loggingInText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		loggingInText.setVisibility(View.VISIBLE);
		
		credentials = new UserCredentials(lastEmail,privateKey, publicKey);
		
		megaApi.fastLogin(lastEmail, publicKey, privateKey, this);
	}
	
	/*
	 * Validate email and password
	 */
	private boolean validateForm() {
		String emailError = getEmailError();
		String passwordError = getPasswordError();

		et_user.setError(emailError);
		et_password.setError(passwordError);

		if (emailError != null) {
			et_user.requestFocus();
			return false;
		} else if (passwordError != null) {
			et_password.requestFocus();
			return false;
		}
		return true;
	}
	
	/*
	 * Validate email
	 */
	private String getEmailError() {
		String value = et_user.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}
	
	/*
	 * Validate password
	 */
	private String getPasswordError() {
		String value = et_password.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_password);
		}
		return null;
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request)
	{
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError error) {
		
		log("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_FAST_LOGIN){
			if (error.getErrorCode() != MegaError.API_OK) {
				String errorMessage;
				if (error.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_incorrect_email_or_password);
				}
				else if (error.getErrorCode() == MegaError.API_ENOENT) {
					errorMessage = getString(R.string.error_server_connection_problem);
				}
				else {
					errorMessage = error.getErrorString();
				}
				loginLoggingIn.setVisibility(View.GONE);
				loginLogin.setVisibility(View.VISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.VISIBLE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				
				Util.showErrorAlertDialog(errorMessage, false, loginActivity);
			}
			else{

				loggingInText.setVisibility(View.VISIBLE);
				fetchingNodesText.setVisibility(View.VISIBLE);
				
				Preferences.saveCredentials(loginActivity, credentials);

				megaApi.fetchNodes(loginActivity);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (error.getErrorCode() != MegaError.API_OK) {
				String errorMessage;
				errorMessage = error.getErrorString();
				loginLoggingIn.setVisibility(View.GONE);
				loginLogin.setVisibility(View.VISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.VISIBLE);
				generatingKeysText.setVisibility(View.GONE);
				loggingInText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				
				Util.showErrorAlertDialog(errorMessage, false, loginActivity);
			}
			else{
				if (!backWhileLogin){
					
					if (parentHandle != -1){
						Intent intent = new Intent();
						intent.putExtra("PARENT_HANDLE", parentHandle);
						setResult(RESULT_OK, intent);
						finish();
					}
					else{
						Intent intent = new Intent(loginActivity,ManagerActivity.class);
						if (action != null){
							intent.setAction(action);
							intent.setData(Uri.parse(url));
						}
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						finish();
					}
				}
			}	
		}
		else if (request.getType() == MegaRequest.TYPE_QUERY_SIGNUP_LINK){
			String s = "";
			loginLogin.setVisibility(View.VISIBLE);
			loginDelimiter.setVisibility(View.VISIBLE);
			loginCreateAccount.setVisibility(View.VISIBLE);
			loginLoggingIn.setVisibility(View.GONE);
			generatingKeysText.setVisibility(View.GONE);
			queryingSignupLinkText.setVisibility(View.GONE);
			confirmingAccountText.setVisibility(View.GONE);
			fetchingNodesText.setVisibility(View.GONE);
			
			if(error.getErrorCode() == MegaError.API_OK){
				s = request.getEmail();
				et_user.setText(s);
				et_password.requestFocus();
			}
			else{
				Util.showErrorAlertDialog(error.getErrorString(), true, LoginActivity.this);
				confirmLink = null;
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FAST_CONFIRM_ACCOUNT){
			if (error.getErrorCode() == MegaError.API_OK){
				log("fastConfirm finished - OK");
				onKeysGeneratedLogin(gPrivateKey, gPublicKey);
			}
			else{
				loginLogin.setVisibility(View.VISIBLE);
				loginDelimiter.setVisibility(View.VISIBLE);
				loginCreateAccount.setVisibility(View.VISIBLE);
				loginLoggingIn.setVisibility(View.GONE);
				generatingKeysText.setVisibility(View.GONE);
				queryingSignupLinkText.setVisibility(View.GONE);
				confirmingAccountText.setVisibility(View.GONE);
				fetchingNodesText.setVisibility(View.GONE);
				
				if (error.getErrorCode() == MegaError.API_ENOENT){
					Util.showErrorAlertDialog(getString(R.string.error_incorrect_email_or_password), false, LoginActivity.this);
				}
				else{
					Util.showErrorAlertDialog(error.getErrorString(), false, LoginActivity.this);
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
	{
		log("onRequestTemporaryError: " + request.getRequestString());
	}
	
	@Override
	public void onBackPressed() {
		backWhileLogin = true;
		
		if (loginClicked){
			super.onBackPressed();
		}
		else{
			Intent intent = new Intent(this, TourActivity.class);
			startActivity(intent);
			finish();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	public void onNewIntent(Intent intent){
		if (intent != null && ACTION_CONFIRM.equals(intent.getAction())) {
			handleConfirmationIntent(intent);
		}
	}
	
	/*
	 * Handle intent from confirmation email
	 */
	private void handleConfirmationIntent(Intent intent) {
		confirmLink = intent.getStringExtra(EXTRA_CONFIRMATION);
		loginTitle.setText(R.string.login_confirm_account);
		bLogin.setText(R.string.login_confirm_account);
		updateConfirmEmail(confirmLink);
	}
	
	/*
	 * Get email address from confirmation code and set to emailView
	 */
	private void updateConfirmEmail(String link) {
		if(!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), true, this);
			return;
		}
		
		loginLogin.setVisibility(View.GONE);
		loginDelimiter.setVisibility(View.GONE);
		loginCreateAccount.setVisibility(View.GONE);
		loginLoggingIn.setVisibility(View.VISIBLE);
		generatingKeysText.setVisibility(View.GONE);
		queryingSignupLinkText.setVisibility(View.VISIBLE);
		confirmingAccountText.setVisibility(View.GONE);
		fetchingNodesText.setVisibility(View.GONE);
		log("querySignupLink");
		megaApi.querySignupLink(link, this);
	}
	
	
	public static void log(String message) {
		Util.log("LoginActivity", message);
	}	
}
